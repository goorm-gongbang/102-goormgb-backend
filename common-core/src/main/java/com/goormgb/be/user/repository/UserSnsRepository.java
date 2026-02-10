package com.goormgb.be.user.repository;

import com.goormgb.be.user.entity.UserSns;
import com.goormgb.be.user.enums.SocialProvider;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UserSnsRepository extends JpaRepository<UserSns, Long> {
	Optional<UserSns> findByProviderAndProviderUserId(
			SocialProvider provider,
			String providerUserId
	);

	Optional<UserSns> findByUserId(Long userId);
}
