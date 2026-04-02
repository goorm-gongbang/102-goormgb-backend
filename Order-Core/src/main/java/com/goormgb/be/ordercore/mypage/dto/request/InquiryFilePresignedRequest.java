package com.goormgb.be.ordercore.mypage.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Schema(description = "1:1 문의 첨부 파일 Presigned URL 발급 요청")
public record InquiryFilePresignedRequest(
	@Schema(description = "첨부 파일명", example = "receipt.jpg", requiredMode = Schema.RequiredMode.REQUIRED)
	@NotBlank(message = "fileName은 필수입니다.")
	@Size(max = 255, message = "fileName은 255자 이하여야 합니다.")
	String fileName
) {
}
