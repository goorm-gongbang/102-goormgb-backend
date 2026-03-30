package com.goormgb.be.ordercore.mypage.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

@Schema(description = "1:1 문의 생성 요청")
public record MyPageInquiryCreateRequest(
	@Schema(description = "문의 카테고리", example = "BOOKING", requiredMode = Schema.RequiredMode.REQUIRED)
	@NotBlank(message = "category는 필수입니다.")
	String category,

	@Schema(description = "문의 제목", example = "좌석 변경이 가능한가요?", requiredMode = Schema.RequiredMode.REQUIRED)
	@NotBlank(message = "title은 필수입니다.")
	@Size(max = 200, message = "title은 200자 이하여야 합니다.")
	String title,

	@Schema(description = "문의 내용", example = "3월 29일 경기 좌석을 변경하고 싶습니다...", requiredMode = Schema.RequiredMode.REQUIRED)
	@NotBlank(message = "content는 필수입니다.")
	String content,

	@Schema(description = "연락처", example = "010-1234-5678")
	@Pattern(
		regexp = "^$|^01[0-9]-?\\d{3,4}-?\\d{4}$",
		message = "phoneNumber 형식이 올바르지 않습니다."
	)
	String phoneNumber
) {
}
