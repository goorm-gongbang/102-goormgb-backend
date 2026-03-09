package com.goormgb.be.global.config;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.module.SimpleModule;

/**
 * Jackson 전역 직렬화 설정.
 * <p>
 * LocalDateTime은 타임존 정보를 갖지 않지만, 프로젝트 전체에서 UTC로 저장/처리한다.
 * (hibernate.jdbc.time_zone: UTC, spring.jackson.time-zone: UTC)
 * <p>
 * 이 설정을 통해 모든 LocalDateTime 필드를 자동으로 UTC ISO 8601 형식으로 직렬화한다.
 * 예: "2026-03-10T14:00:00.000Z"
 * <p>
 * 응답 DTO에서 별도 변환 없이 LocalDateTime을 그대로 사용 가능.
 */
@Configuration
public class JacksonConfig {

	private static final DateTimeFormatter UTC_FORMATTER =
			DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'").withZone(ZoneOffset.UTC);

	@Bean
	public SimpleModule localDateTimeUtcModule() {
		SimpleModule module = new SimpleModule();
		module.addSerializer(LocalDateTime.class, new JsonSerializer<>() {
			@Override
			public void serialize(LocalDateTime value, JsonGenerator gen, SerializerProvider serializers)
					throws IOException {
				gen.writeString(UTC_FORMATTER.format(value));
			}
		});
		return module;
	}
}
