package com.goormgb.be.authguard.auth.controller;

import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.goormgb.be.authguard.auth.dto.DevLoginRequest;
import com.goormgb.be.authguard.auth.service.LoadTestAuthService;
import com.goormgb.be.global.response.ApiResult;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@Tag(name = "Load Test Auth", description = "부하테스트용 회원가입 API (local/dev/test 전용)")
@Profile({"local", "dev", "test"})
@RestController
@RequestMapping("/loadtest")
@RequiredArgsConstructor
public class LoadTestSignupController {

	private final LoadTestAuthService loadTestAuthService;

	@Operation(
			summary = "부하테스트 유저 회원가입",
			description = "부하테스트용 유저를 생성합니다. loginId/password 자유 입력 (예: 1/1, a/a)"
	)
	@PostMapping("/signup")
	public ResponseEntity<ApiResult<Void>> signup(
			@Valid @RequestBody DevLoginRequest request
	) {
		loadTestAuthService.signup(request.getLoginId(), request.getPassword());
		return ResponseEntity.status(HttpStatus.CREATED)
				.body(ApiResult.ok("회원가입 성공", null));
	}
}
