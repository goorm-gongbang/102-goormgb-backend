package com.goormgb.be.ordercore.order.service;

import java.time.DayOfWeek;
import java.time.Instant;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.goormgb.be.domain.match.entity.Match;
import com.goormgb.be.domain.match.repository.MatchRepository;
import com.goormgb.be.global.exception.ErrorCode;
import com.goormgb.be.global.support.Preconditions;
import com.goormgb.be.ordercore.order.dto.request.OrderCreateRequest;
import com.goormgb.be.ordercore.order.dto.request.SeatOrderItem;
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
	private static final int BOOKING_FEE = 2000;

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
		Preconditions.validate(!request.seats().isEmpty(), ErrorCode.ORDER_SEAT_EMPTY);

		User user = userRepository.findByIdOrThrow(userId, ErrorCode.USER_NOT_FOUND);
		Match match = matchRepository.findDetailByIdOrThrow(request.matchId());

		List<Long> matchSeatIds = request.seats().stream()
				.map(SeatOrderItem::matchSeatId)
				.toList();

		List<SeatHoldInfo> holdInfos = seatInfoQueryService.findSeatHoldInfos(userId, matchSeatIds);
		Preconditions.validate(holdInfos.size() == matchSeatIds.size(), ErrorCode.SEAT_HOLD_NOT_FOUND);

		Instant now = Instant.now();
		for (SeatHoldInfo hold : holdInfos) {
			Preconditions.validate(!hold.isExpired(now), ErrorCode.SEAT_HOLD_EXPIRED);
			Preconditions.validate(!seatInfoQueryService.isAlreadyOrdered(hold.matchSeatId()),
					ErrorCode.INVALID_ORDER_STATUS);
		}

		String dayType = determineDayType(match.getMatchAt());
		Map<Long, SeatOrderItem> seatItemMap = request.seats().stream()
				.collect(Collectors.toMap(SeatOrderItem::matchSeatId, item -> item));

		// 가격 미리 계산
		record SeatPriceItem(SeatHoldInfo hold, SeatOrderItem item, int price) {
		}
		List<SeatPriceItem> priceItems = new ArrayList<>();
		int ticketTotal = 0;
		for (SeatHoldInfo hold : holdInfos) {
			SeatOrderItem item = seatItemMap.get(hold.matchSeatId());
			Integer price = seatInfoQueryService.findPrice(hold.sectionId(), dayType, item.ticketType().name());
			Preconditions.validate(price != null, ErrorCode.PRICE_POLICY_NOT_FOUND);
			priceItems.add(new SeatPriceItem(hold, item, price));
			ticketTotal += price;
		}
		int totalAmount = ticketTotal + BOOKING_FEE;

		Order order = Order.builder()
				.user(user)
				.match(match)
				.totalAmount(totalAmount)
				.ordererName(request.ordererName())
				.ordererEmail(request.ordererEmail())
				.ordererPhone(request.ordererPhone())
				.ordererBirthDate(request.ordererBirthDate())
				.build();

		orderRepository.save(order);

		List<OrderSeat> orderSeats = priceItems.stream()
				.map(pi -> OrderSeat.builder()
						.order(order)
						.matchSeatId(pi.hold().matchSeatId())
						.blockId(pi.hold().blockId())
						.sectionId(pi.hold().sectionId())
						.rowNo(pi.hold().rowNo())
						.seatNo(pi.hold().seatNo())
						.price(pi.price())
						.ticketType(pi.item().ticketType())
						.build())
				.toList();

		orderSeatRepository.saveAll(orderSeats);

		log.info("[OrderService] 주문 생성 완료 - orderId={}, userId={}, seatCount={}, totalAmount={}",
				order.getId(), userId, orderSeats.size(), totalAmount);

		return OrderCreateResponse.of(order, orderSeats.size());
	}

	private String determineDayType(Instant matchAt) {
		DayOfWeek dow = matchAt.atZone(KST).getDayOfWeek();
		return (dow == DayOfWeek.FRIDAY || dow == DayOfWeek.SATURDAY || dow == DayOfWeek.SUNDAY)
				? "WEEKEND" : "WEEKDAY";
	}
}
