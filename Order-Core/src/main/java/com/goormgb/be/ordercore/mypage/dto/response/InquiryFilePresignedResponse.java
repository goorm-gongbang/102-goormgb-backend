package com.goormgb.be.ordercore.mypage.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "1:1 문의 첨부 파일 Presigned URL 응답")
public record InquiryFilePresignedResponse(
	@Schema(description = "S3 업로드용 Presigned PUT URL")
	String presignedUrl,
	@Schema(description = "업로드된 파일의 S3 key")
	String fileKey
) {
	public static InquiryFilePresignedResponse of(String presignedUrl, String fileKey) {
		return new InquiryFilePresignedResponse(presignedUrl, fileKey);
	}
}
