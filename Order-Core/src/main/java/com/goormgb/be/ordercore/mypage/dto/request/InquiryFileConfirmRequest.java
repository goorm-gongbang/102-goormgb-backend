package com.goormgb.be.ordercore.mypage.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

@Schema(description = "1:1 문의 첨부 파일 확정 요청")
public record InquiryFileConfirmRequest(
	@Schema(description = "업로드된 파일의 S3 key", example = "dev/123/42/a1b2c3d4.jpg", requiredMode = Schema.RequiredMode.REQUIRED)
	@NotBlank(message = "fileKey는 필수입니다.")
	String fileKey
) {
}
