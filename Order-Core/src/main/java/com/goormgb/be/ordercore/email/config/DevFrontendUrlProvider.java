package com.goormgb.be.ordercore.email.config;

import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

/**
 * dev / local 프로파일에서 사용하는 프론트엔드 URL 제공자.
 */
@Component
@Profile({"dev", "local"})
public class DevFrontendUrlProvider implements FrontendUrlProvider {

	@Override
	public String getBaseUrl() {
		return "https://dev.goormgb.space";
	}
}
