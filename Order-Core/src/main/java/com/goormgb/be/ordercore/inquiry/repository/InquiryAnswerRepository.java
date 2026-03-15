package com.goormgb.be.ordercore.inquiry.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.goormgb.be.ordercore.inquiry.entity.InquiryAnswer;

public interface InquiryAnswerRepository extends JpaRepository<InquiryAnswer, Long> {

	Optional<InquiryAnswer> findByInquiryId(Long inquiryId);
}
