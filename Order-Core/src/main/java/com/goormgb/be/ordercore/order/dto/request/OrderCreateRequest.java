package com.goormgb.be.ordercore.order.dto.request;

import java.util.List;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

@Schema(description = "주문 생성 요청")
public record OrderCreateRequest(

	@Schema(description = "경기 ID", example = "1", requiredMode = Schema.RequiredMode.REQUIRED)
	@NotNull(message = "matchId는 필수입니다.")
	Long matchId,

	@Schema(description = "좌석별 주문 정보 (matchSeatId + ticketType)", requiredMode = Schema.RequiredMode.REQUIRED)
	@NotEmpty(message = "좌석 정보는 최소 1개 이상이어야 합니다.")
	@Valid
	List<SeatOrderItem> seats,

	@Schema(description = "총 결제 금액 (좌석 가격 합계 + 수수료 2,000원, 프론트에서 계산)", example = "42000", requiredMode = Schema.RequiredMode.REQUIRED)
	@NotNull(message = "총 결제 금액은 필수입니다.")
	@Min(value = 0, message = "총 결제 금액은 0 이상이어야 합니다.")
	Integer totalPrice,

	@Schema(description = "예매자 이름", example = "윤정빈", requiredMode = Schema.RequiredMode.REQUIRED)
	@NotBlank(message = "예매자 이름은 필수입니다.")
	@Size(max = 50)
	String ordererName,

	@Schema(description = "예매자 이메일", example = "test@email.com", requiredMode = Schema.RequiredMode.REQUIRED)
	@NotBlank(message = "예매자 이메일은 필수입니다.")
	@Email(message = "이메일 형식이 올바르지 않습니다.")
	@Size(max = 255)
	String ordererEmail,

	@Schema(description = "예매자 휴대폰 번호", example = "01012345678", requiredMode = Schema.RequiredMode.REQUIRED)
	@NotBlank(message = "예매자 휴대폰 번호는 필수입니다.")
	@Size(max = 20)
	String ordererPhone,

	@Schema(description = "예매자 생년월일 (YYMMDD)", example = "990831", requiredMode = Schema.RequiredMode.REQUIRED)
	@NotBlank(message = "예매자 생년월일은 필수입니다.")
	@Pattern(regexp = "^\\d{6}$", message = "생년월일은 6자리 숫자여야 합니다. (예: 990831)")
	String ordererBirthDate
) {
}
