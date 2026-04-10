package com.goormgb.be.ordercore.mypage.service;

import java.util.Locale;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.goormgb.be.global.exception.CustomException;
import com.goormgb.be.global.exception.ErrorCode;
import com.goormgb.be.global.support.Preconditions;
import com.goormgb.be.ordercore.inquiry.entity.Inquiry;
import com.goormgb.be.ordercore.inquiry.enums.InquiryCategory;
import com.goormgb.be.ordercore.inquiry.repository.InquiryRepository;
import com.goormgb.be.ordercore.mypage.dto.request.MyPageInquiryCreateRequest;
import com.goormgb.be.ordercore.mypage.dto.response.MyPageInquiryCreateResponse;
import com.goormgb.be.ordercore.mypage.dto.response.MyPageInquiryDetailResponse;
import com.goormgb.be.ordercore.mypage.dto.response.MyPageInquiryListResponse;
import com.goormgb.be.user.entity.User;
import com.goormgb.be.user.repository.UserRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MyPageInquiryService {

	private final UserRepository userRepository;
	private final InquiryRepository inquiryRepository;

	@Transactional
	public MyPageInquiryCreateResponse createInquiry(Long userId, MyPageInquiryCreateRequest request) {
		User user = userRepository.findByIdOrThrow(userId, ErrorCode.USER_NOT_FOUND);

		Inquiry inquiry = Inquiry.create(
			user,
			parseCategory(request.category()),
			request.title().trim(),
			request.content().trim(),
			normalizePhoneNumber(request.phoneNumber())
		);

		Inquiry saved = inquiryRepository.save(inquiry);
		return MyPageInquiryCreateResponse.of(saved.getId());
	}

	public MyPageInquiryListResponse getInquiries(Long userId) {
		userRepository.findByIdOrThrow(userId, ErrorCode.USER_NOT_FOUND);
		List<Inquiry> inquiries = inquiryRepository.findAllByUserIdOrderByCreatedAtDesc(userId);
		return MyPageInquiryListResponse.of(inquiries);
	}

	public MyPageInquiryDetailResponse getInquiryDetail(Long userId, Long inquiryId) {
		Inquiry inquiry = inquiryRepository.findById(inquiryId)
			.orElseThrow(() -> new CustomException(ErrorCode.INQUIRY_NOT_FOUND));
		Preconditions.validate(inquiry.getUser().getId().equals(userId), ErrorCode.INQUIRY_ACCESS_DENIED);

		return MyPageInquiryDetailResponse.of(inquiry, null);
	}

	private InquiryCategory parseCategory(String rawCategory) {
		Preconditions.validate(rawCategory != null && !rawCategory.isBlank(), ErrorCode.INVALID_INQUIRY_CATEGORY);

		String normalized = rawCategory.trim().toUpperCase(Locale.ROOT);
		try {
			return InquiryCategory.valueOf(normalized);
		} catch (IllegalArgumentException e) {
			throw new CustomException(ErrorCode.INVALID_INQUIRY_CATEGORY, e);
		}
	}

	private String normalizePhoneNumber(String phoneNumber) {
		if (phoneNumber == null) {
			return null;
		}
		String normalized = phoneNumber.trim();
		return normalized.isEmpty() ? null : normalized;
	}
}
