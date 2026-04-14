package com.goormgb.be.ordercore.email.config;

import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

/**
 * staging 프로파일에서 사용하는 프론트엔드 URL 제공자.
 */
@Component
@Profile("staging")
public class StagingFrontendUrlProvider implements FrontendUrlProvider {

	@Override
	public String getBaseUrl() {
		return "https://staging.playball.one";
	}
}
