package com.goormgb.be.global.environment;

import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import com.goormgb.be.global.exception.ErrorCode;

/**
 * 운영 환경용 에러 응답 전략 (staging, prod).
 *
 * <p>ErrorCode를 HTTP 상태 시리즈(CLIENT_ERROR, SERVER_ERROR)로 추상화하여
 * 내부 에러 코드가 외부에 노출되지 않도록 한다.</p>
 *
 * <p>응답 예시:</p>
 * <pre>
 * {
 *   "code": "CLIENT_ERROR",
 *   "message": "온보딩 선호도 정보를 찾을 수 없습니다."
 * }
 * </pre>
 *
 * <p>추후 에러 코드 추상화 작업 시 이 클래스를 확장하여
 * 특정 ErrorCode에 대해 커스텀 코드를 반환하도록 구현할 수 있다.</p>
 *
 * @see ErrorResponseStrategy
 * @see DetailedErrorResponseStrategy
 */
@Component
@Profile({"staging", "prod"})
public class AbstractErrorResponseStrategy implements ErrorResponseStrategy {

	@Override
	public String resolveCode(ErrorCode errorCode) {
		return errorCode.getStatus().series().name();
	}
}
