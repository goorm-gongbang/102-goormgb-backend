package com.goormgb.be.ordercore.email.config;

import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

/**
 * prod / default(프로파일 미지정) 환경에서 사용하는 프론트엔드 URL 제공자.
 *
 * TODO: test 프로파일 등 명시되지 않은 프로파일에서 @SpringBootTest 실행 시
 *       NoSuchBeanDefinitionException 발생 가능. 이메일 통합 테스트 추가 시
 *       @ConditionalOnMissingBean 방식으로 교체 또는 test 프로파일 구현체 추가 필요.
 */
@Component
@Profile({"prod", "default"})
public class DefaultFrontendUrlProvider implements FrontendUrlProvider {

	@Override
	public String getBaseUrl() {
		return "https://playball.one";
	}
}
