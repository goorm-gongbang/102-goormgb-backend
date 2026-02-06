package com.goormgb.be.user.repository;

import com.goormgb.be.user.entity.UserSns;
import com.goormgb.be.user.enums.SocialProvider;
import org.springframework.data.jpa.repository.JpaRepository;

import javax.swing.text.html.Option;
import java.util.Optional;

public interface UserSnsRepository extends JpaRepository<UserSns, Long> {
    /**
     * OAuth 로그인 핵심 조회 메서드
     *
     * provider (KAKAO)
     * providerUserId (카카오 고유 ID)
     *
     * → 기존 회원인지 판단하는 기준
     */
    Optional<UserSns> findByProviderAndProviderUserId(
            SocialProvider provider,
            String providerUserId
    );

    /**
     * 특정 유저에 연결된 소셜 계정 조회
     * (나중에 마이페이지, 연동 해제 등에 사용 가능)
     */
    Optional<UserSns> findByUserId(Long userId);

}
