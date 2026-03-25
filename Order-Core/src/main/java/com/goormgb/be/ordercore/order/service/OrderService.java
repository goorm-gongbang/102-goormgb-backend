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
import com.goormgb.be.ordercore.order.dto.request.OrderCreateRequest;
import com.goormgb.be.ordercore.order.dto.response.OrderCreateResponse;
import com.goormgb.be.ordercore.order.dto.response.OrderSheetGetResponse;
import com.goormgb.be.ordercore.order.entity.Order;
import com.goormgb.be.ordercore.order.entity.OrderSeat;
import com.goormgb.be.ordercore.order.query.SeatHoldInfo;
import com.goormgb.be.ordercore.order.query.SeatInfoQueryService;
import com.goormgb.be.ordercore.order.repository.OrderRepository;
import com.goormgb.be.ordercore.order.repository.OrderSeatRepository;
import com.goormgb.be.user.entity.User;
import com.goormgb.be.user.repository.UserRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class OrderService {

	private static final ZoneId KST = ZoneId.of("Asia/Seoul");

	private final MatchRepository matchRepository;
	private final UserRepository userRepository;
	private final OrderRepository orderRepository;
	private final OrderSeatRepository orderSeatRepository;
	private final SeatInfoQueryService seatInfoQueryService;

	/**
	 * 주문서 조회: 좌석 선점 정보와 경기 정보를 조합하여 반환한다.
	 */
	@Transactional(readOnly = true)
	public OrderSheetGetResponse getOrderSheet(Long userId, Long matchId, List<Long> matchSeatIds) {
		Preconditions.validate(!matchSeatIds.isEmpty(), ErrorCode.ORDER_SEAT_EMPTY);

		Match match = matchRepository.findDetailByIdOrThrow(matchId);

		String dayType = determineDayType(match.getMatchAt());

		List<SeatHoldInfo> holdInfos = seatInfoQueryService.findSeatHoldInfos(userId, matchSeatIds);
		Preconditions.validate(holdInfos.size() == matchSeatIds.size(), ErrorCode.SEAT_HOLD_NOT_FOUND);

		Instant now = Instant.now();
		List<OrderSheetGetResponse.SeatInfo> seatInfos = holdInfos.stream()
				.map(hold -> {
					Preconditions.validate(!hold.isExpired(now), ErrorCode.SEAT_HOLD_EXPIRED);
					Integer adultPrice = seatInfoQueryService.findPrice(hold.sectionId(), dayType, "ADULT");
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

		User user = userRepository.findByIdOrThrow(userId, ErrorCode.USER_NOT_FOUND);
		Match match = matchRepository.findDetailByIdOrThrow(request.matchId());

		List<SeatHoldInfo> holdInfos = seatInfoQueryService.findSeatHoldInfos(userId, request.matchSeatIds());
		Preconditions.validate(holdInfos.size() == request.matchSeatIds().size(), ErrorCode.SEAT_HOLD_NOT_FOUND);

		Instant now = Instant.now();
		String dayType = determineDayType(match.getMatchAt());

		// 유효성 검증 + 좌석별 성인 기본가 조회 (order_seats 저장용)
		List<OrderSeat> orderSeats = new ArrayList<>();
		for (SeatHoldInfo hold : holdInfos) {
			Preconditions.validate(!hold.isExpired(now), ErrorCode.SEAT_HOLD_EXPIRED);
			Preconditions.validate(!seatInfoQueryService.isAlreadyOrdered(hold.matchSeatId()),
					ErrorCode.INVALID_ORDER_STATUS);

			Integer adultPrice = seatInfoQueryService.findPrice(hold.sectionId(), dayType, "ADULT");
			Preconditions.validate(adultPrice != null, ErrorCode.PRICE_POLICY_NOT_FOUND);

			orderSeats.add(OrderSeat.builder()
					.matchSeatId(hold.matchSeatId())
					.blockId(hold.blockId())
					.sectionId(hold.sectionId())
					.rowNo(hold.rowNo())
					.seatNo(hold.seatNo())
					.price(adultPrice)
					.ticketType(TicketType.ADULT)
					.build());
		}

		Order order = Order.builder()
				.user(user)
				.match(match)
				.totalAmount(request.totalPrice())
				.ordererName(request.ordererName())
				.ordererEmail(request.ordererEmail())
				.ordererPhone(request.ordererPhone())
				.ordererBirthDate(request.ordererBirthDate())
				.build();

		orderRepository.save(order);
		orderSeats.forEach(seat -> seat.assignOrder(order));
		orderSeatRepository.saveAll(orderSeats);

		log.info("[OrderService] 주문 생성 완료 - orderId={}, userId={}, seatCount={}, totalAmount={}",
				order.getId(), userId, orderSeats.size(), request.totalPrice());

		return OrderCreateResponse.of(order, orderSeats.size());
	}

	private String determineDayType(Instant matchAt) {
		DayOfWeek dow = matchAt.atZone(KST).getDayOfWeek();
		return (dow == DayOfWeek.FRIDAY || dow == DayOfWeek.SATURDAY || dow == DayOfWeek.SUNDAY)
				? "WEEKEND" : "WEEKDAY";
	}
}
