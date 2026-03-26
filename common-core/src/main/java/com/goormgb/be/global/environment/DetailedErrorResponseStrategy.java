package com.goormgb.be.global.environment;

import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import com.goormgb.be.global.exception.ErrorCode;

/**
 * 개발 환경용 에러 응답 전략 (local, dev, test).
 *
 * <p>ErrorCode enum 이름을 그대로 에러 코드로 반환하여 디버깅을 용이하게 한다.</p>
 *
 * <p>응답 예시:</p>
 * <pre>
 * {
 *   "code": "PREFERENCE_NOT_FOUND",
 *   "message": "온보딩 선호도 정보를 찾을 수 없습니다."
 * }
 * </pre>
 *
 * @see ErrorResponseStrategy
 * @see AbstractErrorResponseStrategy
 */
@Component
@Profile({"local", "dev", "test"})
public class DetailedErrorResponseStrategy implements ErrorResponseStrategy {

	@Override
	public String resolveCode(ErrorCode errorCode) {
		return errorCode.name();
	}
}
