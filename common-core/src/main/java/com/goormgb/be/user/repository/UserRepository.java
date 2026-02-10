package com.goormgb.be.user.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.goormgb.be.global.exception.CustomException;
import com.goormgb.be.global.exception.ErrorCode;
import com.goormgb.be.user.entity.User;

public interface UserRepository extends JpaRepository<User, Long> {
	default User findByIdOrThrow(Long id, ErrorCode errorCode) {
		return findById(id).orElseThrow(() -> new CustomException(errorCode));
	}
}
