package com.goormgb.be.ordercore.mypage.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record MyPageAccountUpdateRequest(
	@NotBlank(message = "닉네임은 공백일 수 없습니다.")
	@Size(max = 15, message = "닉네임은 15자 이하여야 합니다.")
	String nickname
) {
}
