package com.goormgb.be.ordercore.email.config;

import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

/**
 * prod / default(프로파일 미지정) 환경에서 사용하는 프론트엔드 URL 제공자.
 */
@Component
@Profile({"prod", "default"})
public class DefaultFrontendUrlProvider implements FrontendUrlProvider {

	@Override
	public String getBaseUrl() {
		return "https://playball.one";
	}
}
