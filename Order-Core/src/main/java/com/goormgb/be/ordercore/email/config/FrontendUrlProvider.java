package com.goormgb.be.ordercore.email.config;

/**
 * 이메일 템플릿에서 사용할 프론트엔드 기본 URL을 제공하는 인터페이스.
 * 활성 Spring 프로파일에 따라 적절한 구현체가 주입된다.
 */
public interface FrontendUrlProvider {

	String getBaseUrl();
}
