package com.goormgb.be.ordercore.order.dto.request;

import java.time.LocalDate;
import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record OrderCreateRequest(

	@NotNull(message = "matchId는 필수입니다.")
	Long matchId,

	@NotEmpty(message = "좌석 정보는 최소 1개 이상이어야 합니다.")
	@Valid
	List<SeatOrderItem> seats,

	@NotBlank(message = "예매자 이름은 필수입니다.")
	@Size(max = 50)
	String ordererName,

	@NotBlank(message = "예매자 이메일은 필수입니다.")
	@Email(message = "이메일 형식이 올바르지 않습니다.")
	@Size(max = 255)
	String ordererEmail,

	@NotBlank(message = "예매자 휴대폰 번호는 필수입니다.")
	@Size(max = 20)
	String ordererPhone,

	@NotNull(message = "예매자 생년월일은 필수입니다.")
	LocalDate ordererBirthDate
) {
}
