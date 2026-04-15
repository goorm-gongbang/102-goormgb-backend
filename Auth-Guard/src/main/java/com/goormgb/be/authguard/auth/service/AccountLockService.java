package com.goormgb.be.authguard.auth.service;

import java.time.Duration;
import java.util.Optional;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import com.goormgb.be.global.exception.CustomException;
import com.goormgb.be.global.exception.ErrorCode;

import lombok.extern.slf4j.Slf4j;

/**
 * 계정 단위 로그인 실패 추적 + 잠금 서비스.
 *
 * Rate Limit 이 IP 기반이라 크리덴셜 스터핑(IP 분산 + 동일 계정 공격)에 취약한
 * 점을 보완. 같은 email 에 대한 실패 횟수를 Redis 에 누적하여 짧은 주기 잠금과
 * 장기 누적 잠금을 2단계로 적용.
 *
 * <h3>정책</h3>
 * <ul>
 *   <li>단기 윈도우(10분) — 실패 5회 시 15분 일시 잠금</li>
 *   <li>장기 윈도우(30일) — 실패 20회 시 영구 잠금 (관리자 수동 해제까지)</li>
 * </ul>
 *
 * <h3>Redis 키</h3>
 * <pre>
 *   auth:fail:{email}       TTL 10m  (short window counter)
 *   auth:lock:{email}       TTL 15m  (temporary lock flag, 존재 시 즉시 거부)
 *   auth:fail:30d:{email}   TTL 30d  (long window counter, 누적)
 *   auth:lock:perm:{email}  TTL none (permanent lock flag; DB 이관 고려)
 * </pre>
 *
 * <h3>사용 흐름</h3>
 * <pre>
 *   ensureNotLocked(email)      // 로그인 시도 전 — 잠겨있으면 예외
 *   recordFailure(email)        // 로그인 실패 시 — 카운터 증가
 *   resetOnSuccess(email)       // 로그인 성공 시 — 단기 카운터/잠금 삭제
 * </pre>
 *
 * Kakao OAuth 는 비밀번호가 카카오 측에 있으므로 본 서비스 대상 아님.
 * 대상: dev/auth, loadtest/* 등 이메일+비밀번호 로그인 경로.
 */
@Slf4j
@Service
public class AccountLockService {

	private static final String FAIL_SHORT_PREFIX = "auth:fail:";
	private static final String FAIL_LONG_PREFIX = "auth:fail:30d:";
	private static final String LOCK_TEMP_PREFIX = "auth:lock:";
	private static final String LOCK_PERM_PREFIX = "auth:lock:perm:";

	private static final Duration SHORT_WINDOW = Duration.ofMinutes(10);
	private static final Duration LONG_WINDOW = Duration.ofDays(30);
	private static final Duration TEMP_LOCK_DURATION = Duration.ofMinutes(15);

	private static final long SHORT_THRESHOLD = 5L;
	private static final long LONG_THRESHOLD = 20L;

	private final StringRedisTemplate redis;

	public AccountLockService(StringRedisTemplate redis) {
		this.redis = redis;
	}

	/**
	 * 로그인 시도 전 호출. 잠금 상태면 CustomException(ACCOUNT_*_LOCKED) 발생.
	 * GlobalExceptionHandler 가 ErrorCode.status 로 HTTP 상태 반환 (429/403).
	 */
	public void ensureNotLocked(String email) {
		if (email == null || email.isBlank()) return;

		Boolean perm = redis.hasKey(LOCK_PERM_PREFIX + normalize(email));
		if (Boolean.TRUE.equals(perm)) {
			log.warn("Permanent-locked account access attempt. email={}", mask(email));
			throw new CustomException(ErrorCode.ACCOUNT_PERMANENTLY_LOCKED);
		}

		String tempKey = LOCK_TEMP_PREFIX + normalize(email);
		Boolean temp = redis.hasKey(tempKey);
		if (Boolean.TRUE.equals(temp)) {
			Long ttl = redis.getExpire(tempKey);
			long retry = (ttl != null && ttl > 0) ? ttl : TEMP_LOCK_DURATION.getSeconds();
			log.warn("Temp-locked account access attempt. email={}, retry={}s", mask(email), retry);
			throw new CustomException(ErrorCode.ACCOUNT_TEMPORARILY_LOCKED);
		}
	}

	/**
	 * 로그인 실패 시 호출. 단/장기 카운터 증가 후 임계치 초과 시 잠금 설정.
	 * Redis 장애 시 로그만 남기고 통과 (인증 흐름 막지 않음).
	 */
	public void recordFailure(String email) {
		if (email == null || email.isBlank()) return;
		String norm = normalize(email);

		try {
			long shortCount = incrementWithExpire(FAIL_SHORT_PREFIX + norm, SHORT_WINDOW);
			long longCount = incrementWithExpire(FAIL_LONG_PREFIX + norm, LONG_WINDOW);

			if (shortCount >= SHORT_THRESHOLD) {
				redis.opsForValue().set(LOCK_TEMP_PREFIX + norm, "1", TEMP_LOCK_DURATION);
				log.warn("Temporary lock triggered. email={}, shortCount={}", mask(email), shortCount);
			}

			if (longCount >= LONG_THRESHOLD) {
				redis.opsForValue().set(LOCK_PERM_PREFIX + norm, "1");
				log.warn("Permanent lock triggered. email={}, longCount={}", mask(email), longCount);
			}
		} catch (Exception e) {
			log.error("AccountLock recordFailure failed. email={}, error={}", mask(email), e.getMessage(), e);
		}
	}

	/**
	 * 로그인 성공 시 호출. 단기 카운터/잠금만 초기화 (장기 카운터/영구 잠금은 유지).
	 */
	public void resetOnSuccess(String email) {
		if (email == null || email.isBlank()) return;
		String norm = normalize(email);
		try {
			redis.delete(FAIL_SHORT_PREFIX + norm);
			redis.delete(LOCK_TEMP_PREFIX + norm);
		} catch (Exception e) {
			log.error("AccountLock resetOnSuccess failed. email={}, error={}", mask(email), e.getMessage(), e);
		}
	}

	/**
	 * 관리자 수동 잠금 해제 (운영 도구/Slack bot 연동 대비).
	 */
	public void unlock(String email) {
		if (email == null || email.isBlank()) return;
		String norm = normalize(email);
		try {
			redis.delete(FAIL_SHORT_PREFIX + norm);
			redis.delete(FAIL_LONG_PREFIX + norm);
			redis.delete(LOCK_TEMP_PREFIX + norm);
			redis.delete(LOCK_PERM_PREFIX + norm);
			log.info("Account unlocked by admin. email={}", mask(email));
		} catch (Exception e) {
			log.error("AccountLock unlock failed. email={}, error={}", mask(email), e.getMessage(), e);
		}
	}

	/** 운영/디버깅용 현재 상태 조회 (카운터 값 + 잠금 여부). */
	public LockStatus status(String email) {
		String norm = normalize(email);
		long shortCount = Optional.ofNullable(redis.opsForValue().get(FAIL_SHORT_PREFIX + norm))
			.map(Long::parseLong).orElse(0L);
		long longCount = Optional.ofNullable(redis.opsForValue().get(FAIL_LONG_PREFIX + norm))
			.map(Long::parseLong).orElse(0L);
		boolean temp = Boolean.TRUE.equals(redis.hasKey(LOCK_TEMP_PREFIX + norm));
		boolean perm = Boolean.TRUE.equals(redis.hasKey(LOCK_PERM_PREFIX + norm));
		return new LockStatus(shortCount, longCount, temp, perm);
	}

	private long incrementWithExpire(String key, Duration ttl) {
		Long value = redis.opsForValue().increment(key);
		if (value != null && value == 1L) {
			redis.expire(key, ttl);
		}
		return value == null ? 0L : value;
	}

	private String normalize(String email) {
		return email.trim().toLowerCase();
	}

	/** 로그에 이메일 전체 노출 방지 (개인정보). */
	private String mask(String email) {
		int at = email.indexOf('@');
		if (at <= 1) return "***" + email.substring(Math.max(at, 0));
		return email.charAt(0) + "***" + email.substring(at);
	}

	public record LockStatus(
		long shortWindowFailures,
		long longWindowFailures,
		boolean temporarilyLocked,
		boolean permanentlyLocked
	) {}
}
