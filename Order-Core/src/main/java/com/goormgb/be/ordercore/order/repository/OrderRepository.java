package com.goormgb.be.ordercore.order.repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.goormgb.be.ordercore.order.entity.Order;
import com.goormgb.be.ordercore.order.enums.OrderStatus;

import jakarta.persistence.LockModeType;

public interface OrderRepository extends JpaRepository<Order, Long> {

	Optional<Order> findByIdAndUserId(Long id, Long userId);

	boolean existsByIdAndStatus(Long id, OrderStatus status);

	long countByUserId(Long userId);

	@Query("SELECT COUNT(o) FROM Order o WHERE o.user.id = :userId AND o.status IN :statuses AND o.match.matchAt > :now")
	long countUpcomingOrders(@Param("userId") Long userId,
		@Param("statuses") List<OrderStatus> statuses,
		@Param("now") Instant now);

	@Query("SELECT COUNT(o) FROM Order o WHERE o.user.id = :userId AND o.status = 'PAID' AND o.match.matchAt < :now")
	long countCompletedOrders(@Param("userId") Long userId, @Param("now") Instant now);

	@Query("SELECT COUNT(o) FROM Order o WHERE o.user.id = :userId AND o.status IN :statuses")
	long countByUserIdAndStatusIn(@Param("userId") Long userId, @Param("statuses") List<OrderStatus> statuses);

	@Query("""
		SELECT new com.goormgb.be.ordercore.order.repository.OrderMyPageSummaryCounts(
			COUNT(o),
			COALESCE(SUM(CASE WHEN o.status IN :upcomingStatuses AND o.match.matchAt > :now THEN 1 ELSE 0 END), 0),
			COALESCE(SUM(CASE WHEN o.status IN :cancelRefundStatuses THEN 1 ELSE 0 END), 0),
			COALESCE(SUM(CASE WHEN o.status IN :cancelProcessingStatuses THEN 1 ELSE 0 END), 0),
			COALESCE(SUM(CASE WHEN o.status = :completedStatus AND o.match.matchAt < :now THEN 1 ELSE 0 END), 0)
		)
		FROM Order o
		WHERE o.user.id = :userId
		""")
	OrderMyPageSummaryCounts findMyPageSummaryCounts(
		@Param("userId") Long userId,
		@Param("upcomingStatuses") List<OrderStatus> upcomingStatuses,
		@Param("cancelRefundStatuses") List<OrderStatus> cancelRefundStatuses,
		@Param("cancelProcessingStatuses") List<OrderStatus> cancelProcessingStatuses,
		@Param("completedStatus") OrderStatus completedStatus,
		@Param("now") Instant now
	);

	@Query("""
		SELECT o.id FROM Order o
		WHERE o.user.id = :userId
		  AND o.match.id = :matchId
		  AND o.status = :status
		""")
	List<Long> findIdsByUserIdAndMatchIdAndStatus(
		@Param("userId") Long userId,
		@Param("matchId") Long matchId,
		@Param("status") OrderStatus status
	);

	@Modifying(clearAutomatically = true)
	@Query("""
		UPDATE Order o
		SET o.status = :newStatus
		WHERE o.user.id = :userId
		  AND o.match.id = :matchId
		  AND o.status = :oldStatus
		""")
	int bulkUpdateStatus(
		@Param("userId") Long userId,
		@Param("matchId") Long matchId,
		@Param("oldStatus") OrderStatus oldStatus,
		@Param("newStatus") OrderStatus newStatus
	);

	/**
	 * 특정 유저의 주문 중 지정된 상태에 해당하는 주문을 UNDER_REVIEW로 일괄 변경한다.
	 */
	@Modifying(clearAutomatically = true)
	@Query("""
		UPDATE Order o
		SET o.status = :newStatus
		WHERE o.user.id = :userId
		  AND o.status IN :statuses
		""")
	int bulkUpdateStatusByUserIdAndStatuses(
		@Param("userId") Long userId,
		@Param("statuses") List<OrderStatus> statuses,
		@Param("newStatus") OrderStatus newStatus
	);

	@Lock(LockModeType.PESSIMISTIC_WRITE)
	@Query("""
		SELECT o
		FROM Order o
		JOIN FETCH o.user u
		JOIN FETCH o.match m
		JOIN FETCH m.homeClub hc
		JOIN FETCH m.awayClub ac
		JOIN FETCH m.stadium s
		WHERE o.id = :orderId
		""")
	Optional<Order> findByIdForUpdate(@Param("orderId") Long orderId);

	@Lock(LockModeType.PESSIMISTIC_WRITE)
	@Query("""
		SELECT o
		FROM Order o
		WHERE o.id = :orderId
		  AND o.user.id = :userId
		""")
	Optional<Order> findByIdForUpdateAndUserId(@Param("orderId") Long orderId, @Param("userId") Long userId);

	@Query("""
		SELECT o
		FROM Order o
		JOIN FETCH o.match m
		JOIN FETCH m.homeClub
		JOIN FETCH m.awayClub
		JOIN FETCH m.stadium
		WHERE o.id = :orderId
		""")
	Optional<Order> findByIdWithMatchDetails(@Param("orderId") Long orderId);

	@Query("""
		SELECT o
		FROM Order o
		JOIN FETCH o.match m
		JOIN FETCH m.homeClub
		JOIN FETCH m.awayClub
		JOIN FETCH m.stadium
		WHERE o.user.id = :userId
		  AND o.status IN :statuses
		  AND m.matchAt > :now
		""")
	List<Order> findUpcomingOrdersByUserIdAndStatuses(
		@Param("userId") Long userId,
		@Param("statuses") List<OrderStatus> statuses,
		@Param("now") Instant now
	);
}
