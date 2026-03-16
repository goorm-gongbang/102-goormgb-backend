package com.goormgb.be.authguard.auth.controller;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import com.goormgb.be.global.response.ApiResult;
import com.goormgb.be.user.dto.response.UserInfoGetResponse;
import com.goormgb.be.user.service.UserService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;

@Tag(name = "Users", description = "유저 API")
@RestController
@RequiredArgsConstructor
public class UserController {

	private final UserService userService;

	@Operation(summary = "내 정보 조회", description = "현재 로그인한 유저의 기본 정보(ID, 이메일, 닉네임, 상태)를 조회합니다.",
		security = @SecurityRequirement(name = "BearerAuth"))
	@ApiResponses({
		@ApiResponse(responseCode = "200", description = "조회 성공"),
		@ApiResponse(responseCode = "401", description = "인증 필요", content = @Content)
	})
	@GetMapping("/me")
	public ApiResult<UserInfoGetResponse> getMyInfo(@AuthenticationPrincipal Long id) {
		return ApiResult.ok(userService.getMyInfo(id));
	}
}
