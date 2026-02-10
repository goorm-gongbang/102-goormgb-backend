package com.goormgb.be.authguard.auth.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class DevSignupRequest {

	@NotBlank(message = "로그인 ID는 필수입니다.")
	private String loginId;

	@NotBlank(message = "비밀번호는 필수입니다.")
	private String password;

	private String nickname;

	private String email;
}
