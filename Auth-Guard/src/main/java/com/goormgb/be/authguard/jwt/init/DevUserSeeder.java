package com.goormgb.be.authguard.jwt.init;

import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.goormgb.be.authguard.auth.service.DevAuthService;
import com.goormgb.be.user.repository.DevUserRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@Profile({"local", "dev"})
@RequiredArgsConstructor
public class DevUserSeeder implements CommandLineRunner {

	private final DevUserRepository devUserRepository;
	private final DevAuthService devAuthService;

	@Override
	@Transactional
	public void run(String... args) {
		if (!devUserRepository.existsByLoginId("dev")) {
			devAuthService.signup("dev", "1234", "개발자", "dev@test.com");
			log.info("Dev seed user created - loginId: dev, password: 1234");
		}
	}
}
