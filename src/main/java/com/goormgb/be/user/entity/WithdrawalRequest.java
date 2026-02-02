package com.goormgb.be.user.entity;

import com.goormgb.be.user.enums.WithdrawStatus;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;

@Entity
@Table(name = "withdrawal_requests", indexes = {
        @Index(name = "idx_withdrawal_requests_effective_at", columnList = "effective_at")
})
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class WithdrawalRequest {

    private static final int GRACE_PERIOD_DAYS = 30;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private User user;

    @Column(name = "requested_at", nullable = false)
    private OffsetDateTime requestedAt;

    @Column(name = "effective_at", nullable = false)
    private OffsetDateTime effectiveAt;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private WithdrawStatus status = WithdrawStatus.REQUESTED;

    @Column(name = "cancelled_at")
    private OffsetDateTime cancelledAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = OffsetDateTime.now();
    }

    @Builder
    public WithdrawalRequest(User user) {
        this.user = user;
        this.requestedAt = OffsetDateTime.now();
        this.effectiveAt = this.requestedAt.plusDays(GRACE_PERIOD_DAYS);
        this.status = WithdrawStatus.REQUESTED;
    }

    // public void cancel() {
	// 	// 우리 서비스에서 탈퇴 취소는 없음.
    //     this.status = WithdrawStatus.CANCELLED;
    //     this.cancelledAt = OffsetDateTime.now();
    // }

    public boolean isExpired() {
        return OffsetDateTime.now().isAfter(this.effectiveAt);
    }
}