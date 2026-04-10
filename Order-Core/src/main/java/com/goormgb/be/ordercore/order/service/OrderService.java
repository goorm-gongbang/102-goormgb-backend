package com.goormgb.be.ordercore.order.service;

import java.time.DayOfWeek;
import java.time.Instant;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.goormgb.be.domain.match.entity.Match;
import com.goormgb.be.domain.match.repository.MatchRepository;
import com.goormgb.be.domain.ticket.enums.TicketType;
import com.goormgb.be.global.exception.ErrorCode;
import com.goormgb.be.global.support.Preconditions;
import com.goormgb.be.ordercore.metrics.OrderMetricsService;
import com.goormgb.be.ordercore.metrics.enums.OrderDraftEntryPoint;
import com.goormgb.be.ordercore.order.client.SeatInternalClient;
import com.goormgb.be.ordercore.order.dto.request.OrderCreateRequest;
import com.goormgb.be.ordercore.order.dto.response.OrderCreateResponse;
import com.goormgb.be.ordercore.order.dto.response.OrderSheetGetResponse;
import com.goormgb.be.ordercore.order.entity.Order;
import com.goormgb.be.ordercore.order.entity.OrderSeat;
import com.goormgb.be.ordercore.order.enums.OrderStatus;
import com.goormgb.be.ordercore.order.query.SeatHoldInfo;
import com.goormgb.be.ordercore.order.query.SeatInfoQueryService;
import com.goormgb.be.ordercore.order.repository.OrderRepository;
import com.goormgb.be.ordercore.order.repository.OrderSeatRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class OrderService {

	private static final ZoneId KST = ZoneId.of("Asia/Seoul");
	private static final int BOOKING_FEE = 2000;
	private static final int MAX_TICKETS_PER_MATCH = 8;
	private static final List<OrderStatus> COUNTABLE_ORDER_STATUSES = List.of(
			OrderStatus.PAYMENT_PENDING, OrderStatus.PAID, OrderStatus.UNDER_REVIEW
	);

	private final MatchRepository matchRepository;
	private final OrderRepository orderRepository;
	private final OrderSeatRepository orderSeatRepository;
	private final SeatInternalClient seatInternalClient;
	private final SeatInfoQueryService seatInfoQueryService;
	private final OrderMetricsService orderMetricsService;

	/**
	 * 주문서 조회: 좌석 선점 정보와 경기 정보를 조합하여 반환한다.
	 */
	@Transactional(readOnly = true)
	public OrderSheetGetResponse getOrderSheet(
			Long userId,
			Long matchId,
			List<Long> matchSeatIds,
			OrderDraftEntryPoint entryPoint
	) {
		orderMetricsService.increaseOrderDraftEnter(entryPoint);
		Preconditions.validate(!matchSeatIds.isEmpty(), ErrorCode.ORDER_SEAT_EMPTY);

		Match match = matchRepository.findDetailByIdOrThrow(matchId);

		String dayType = determineDayType(match.getMatchAt());

		List<SeatHoldInfo> holdInfos = seatInternalClient.findSeatHoldInfos(userId, matchId, matchSeatIds);
		Preconditions.validate(holdInfos.size() == matchSeatIds.size(), ErrorCode.SEAT_HOLD_NOT_FOUND);

		Instant now = Instant.now();
		List<OrderSheetGetResponse.SeatInfo> seatInfos = holdInfos.stream()
				.map(hold -> {
					Preconditions.validate(!hold.isExpired(now), ErrorCode.SEAT_HOLD_EXPIRED);
					Integer adultPrice = seatInternalClient.findPrice(hold.sectionId(), dayType, "ADULT");
					Preconditions.validate(adultPrice != null, ErrorCode.PRICE_POLICY_NOT_FOUND);
					return OrderSheetGetResponse.SeatInfo.of(hold, adultPrice);
				})
				.toList();

		return OrderSheetGetResponse.of(match, seatInfos);
	}

	/**
	 * 주문 생성: 좌석 선점 검증 후 Order + OrderSeat 엔티티를 생성한다.
	 */
	public OrderCreateResponse createOrder(Long userId, OrderCreateRequest request) {
		Preconditions.validate(!request.matchSeatIds().isEmpty(), ErrorCode.ORDER_SEAT_EMPTY);

		cancelExistingPendingOrders(userId, request.matchId());
		validateMaxTicketsPerMatch(userId, request.matchId(), request.matchSeatIds().size());

		Match match = matchRepository.findDetailByIdOrThrow(request.matchId());

		List<SeatHoldInfo> holdInfos = seatInternalClient.findSeatHoldInfos(userId, request.matchId(), request.matchSeatIds());
		Preconditions.validate(holdInfos.size() == request.matchSeatIds().size(), ErrorCode.SEAT_HOLD_NOT_FOUND);

		Instant now = Instant.now();
		String dayType = determineDayType(match.getMatchAt());

		List<OrderSeat> orderSeats = new ArrayList<>();
		int totalSeatPrice = 0;
		for (SeatHoldInfo hold : holdInfos) {
			Preconditions.validate(!hold.isExpired(now), ErrorCode.SEAT_HOLD_EXPIRED);
			Preconditions.validate(!seatInfoQueryService.isAlreadyOrdered(hold.matchSeatId()),
					ErrorCode.INVALID_ORDER_STATUS);

			Integer adultPrice = seatInternalClient.findPrice(hold.sectionId(), dayType, "ADULT");
			Preconditions.validate(adultPrice != null, ErrorCode.PRICE_POLICY_NOT_FOUND);
			totalSeatPrice += adultPrice;

			orderSeats.add(OrderSeat.builder()
					.matchSeatId(hold.matchSeatId())
					.blockId(hold.blockId())
					.sectionId(hold.sectionId())
					.rowNo(hold.rowNo())
					.seatNo(hold.seatNo())
					.price(adultPrice)
					.ticketType(TicketType.ADULT)
					.sectionName(hold.sectionName())
					.blockCode(hold.blockCode())
					.build());
		}
		int serverCalculatedTotal = totalSeatPrice + BOOKING_FEE;
		Preconditions.validate(serverCalculatedTotal == request.totalPrice(), ErrorCode.ORDER_TOTAL_PRICE_MISMATCH);

		String matchTitle = match.getHomeClub().getKoName() + " vs " + match.getAwayClub().getKoName();

		Order order = Order.builder()
				.userId(userId)
				.matchId(request.matchId())
				.totalAmount(serverCalculatedTotal)
				.ordererName(request.ordererName())
				.ordererEmail(request.ordererEmail())
				.ordererPhone(request.ordererPhone())
				.ordererBirthDate(request.ordererBirthDate())
				.matchTitle(matchTitle)
				.matchDate(match.getMatchAt())
				.stadiumName(match.getStadium().getKoName())
				.homeClubName(match.getHomeClub().getKoName())
				.awayClubName(match.getAwayClub().getKoName())
				.build();

		orderRepository.save(order);
		orderSeats.forEach(seat -> seat.assignOrder(order));
		orderSeatRepository.saveAll(orderSeats);

		log.info("[OrderService] 주문 생성 완료 - orderId={}, userId={}, seatCount={}, totalAmount={}",
				order.getId(), userId, orderSeats.size(), request.totalPrice());

		return OrderCreateResponse.of(order, orderSeats.size());
	}

	/**
	 * 차단된 유저의 결제 완료(PAID) 및 입금 대기(PAYMENT_PENDING) 주문을 정밀 확인 중(UNDER_REVIEW)으로 일괄 변경한다.
	 */
	public int markOrdersUnderReviewByBlockedUser(Long userId) {
		List<OrderStatus> targetStatuses = List.of(OrderStatus.PAID, OrderStatus.PAYMENT_PENDING);

		int updatedCount = orderRepository.bulkUpdateStatusByUserIdAndStatuses(
				userId, targetStatuses, OrderStatus.UNDER_REVIEW);

		log.info("[OrderService] 차단 유저 주문 상태 UNDER_REVIEW 전환 - userId={}, updatedCount={}",
				userId, updatedCount);

		return updatedCount;
	}

	/**
	 * 경기당 1인 최대 예매 수량(8매)을 초과하는지 검증한다.
	 */
	private void validateMaxTicketsPerMatch(Long userId, Long matchId, int newSeatCount) {
		long existingSeatCount = orderSeatRepository.countByUserIdAndMatchIdAndStatuses(
				userId, matchId, COUNTABLE_ORDER_STATUSES);

		Preconditions.validate(
				existingSeatCount + newSeatCount <= MAX_TICKETS_PER_MATCH,
				ErrorCode.EXCEEDED_MAX_TICKETS_PER_MATCH
		);
	}

	private void cancelExistingPendingOrders(Long userId, Long matchId) {
		List<Long> pendingOrderIds = orderRepository.findIdsByUserIdAndMatchIdAndStatus(
				userId, matchId, OrderStatus.PAYMENT_PENDING);

		if (pendingOrderIds.isEmpty()) {
			return;
		}

		int deletedSeats = orderSeatRepository.deleteByOrderIdIn(pendingOrderIds);
		int cancelledCount = orderRepository.bulkUpdateStatus(
				userId, matchId, OrderStatus.PAYMENT_PENDING, OrderStatus.CANCELLED);

		log.info("[OrderService] 미결제 주문 {}건 자동 취소 (좌석 {}건 삭제) - userId={}, matchId={}",
				cancelledCount, deletedSeats, userId, matchId);
	}

	private String determineDayType(Instant matchAt) {
		DayOfWeek dow = matchAt.atZone(KST).getDayOfWeek();
		return (dow == DayOfWeek.FRIDAY || dow == DayOfWeek.SATURDAY || dow == DayOfWeek.SUNDAY)
				? "WEEKEND" : "WEEKDAY";
	}
}
