package com.goormgb.be.auth.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class DevLoginRequest {

	@Schema(example = "dev")
	@NotBlank(message = "로그인 ID는 필수입니다.")
	private String loginId;

	@Schema(example = "1234")
	@NotBlank(message = "비밀번호는 필수입니다.")
	private String password;
}
