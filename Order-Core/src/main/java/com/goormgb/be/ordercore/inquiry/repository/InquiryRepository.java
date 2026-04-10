package com.goormgb.be.ordercore.inquiry.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import com.goormgb.be.ordercore.inquiry.entity.Inquiry;

public interface InquiryRepository extends JpaRepository<Inquiry, Long> {

	Page<Inquiry> findByUserId(Long userId, Pageable pageable);

	List<Inquiry> findAllByUserIdOrderByCreatedAtDesc(Long userId);

	Optional<Inquiry> findByIdAndUserId(Long inquiryId, Long userId);
}
