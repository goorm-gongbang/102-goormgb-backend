package com.goormgb.be.authguard.auth.controller;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import com.goormgb.be.global.response.ApiResult;
import com.goormgb.be.user.dto.response.UserInfoGetResponse;
import com.goormgb.be.user.service.UserService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;

@Tag(name = "Users", description = "유저 API")
@RestController
@RequiredArgsConstructor
public class UserController {

	private final UserService userService;

	@Operation(summary = "유저 정보 조회", description = "로그인 된 유저의 내 정보 조회 API")
	@GetMapping("/me")
	public ApiResult<UserInfoGetResponse> getMyInfo(@AuthenticationPrincipal Long id) {
		return ApiResult.ok(userService.getMyInfo(id));
	}
}
