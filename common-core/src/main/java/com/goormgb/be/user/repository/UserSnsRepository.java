package com.goormgb.be.user.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.goormgb.be.user.entity.UserSns;
import com.goormgb.be.user.enums.SocialProvider;

public interface UserSnsRepository extends JpaRepository<UserSns, Long> {
	Optional<UserSns> findByProviderAndProviderUserId(
			SocialProvider provider,
			String providerUserId
	);

	Optional<UserSns> findByUserId(Long userId);
}
