package com.goormgb.be.queue;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@ActiveProfiles("test")
@Disabled("Redis 연결 필요 - 통합 테스트 환경 구성 후 활성화")
public class QueueApplicationTests {
	@Test
	void contextLoads() {
	}
}