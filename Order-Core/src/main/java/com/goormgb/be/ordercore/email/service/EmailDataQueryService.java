package com.goormgb.be.ordercore.email.service;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.goormgb.be.domain.match.entity.Match;
import com.goormgb.be.kafka.event.OrderCancelledEvent;
import com.goormgb.be.kafka.event.PaymentCompletedEvent;
import com.goormgb.be.ordercore.email.dto.SeatDisplayInfo;
import com.goormgb.be.ordercore.order.entity.Order;
import com.goormgb.be.ordercore.order.entity.OrderSeat;
import com.goormgb.be.ordercore.order.enums.OrderStatus;
import com.goormgb.be.ordercore.order.repository.OrderRepository;
import com.goormgb.be.ordercore.order.repository.OrderSeatRepository;
import com.goormgb.be.ordercore.payment.entity.Payment;
import com.goormgb.be.ordercore.payment.repository.PaymentRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class EmailDataQueryService {

	private final OrderRepository orderRepository;
	private final OrderSeatRepository orderSeatRepository;
	private final PaymentRepository paymentRepository;
	private final NamedParameterJdbcTemplate jdbcTemplate;

	private static final ZoneId KST = ZoneId.of("Asia/Seoul");
	private static final DateTimeFormatter DATE_FORMATTER =
		DateTimeFormatter.ofPattern("yyyy년 M월 d일 (E) HH:mm", Locale.KOREAN);

	private static final Map<String, String> PAYMENT_METHOD_NAMES = Map.of(
		"TOSS_PAY", "토스페이 사용",
		"KAKAO_PAY", "카카오페이 사용",
		"BANK_TRANSFER", "무통장 입금"
	);

	@Transactional(readOnly = true)
	public Optional<Map<String, Object>> buildPaymentEmailContext(Long orderId, PaymentCompletedEvent event) {
		Order order = orderRepository.findByIdWithMatchDetails(orderId).orElse(null);
		if (order == null) {
			return Optional.empty();
		}

		Match match = order.getMatch();
		List<OrderSeat> orderSeats = orderSeatRepository.findByOrderId(orderId);
		Payment payment = paymentRepository.findByOrderId(orderId).orElse(null);
		List<SeatDisplayInfo> seats = buildSeatDisplayInfos(orderSeats);

		Map<String, Object> ctx = new HashMap<>();
		ctx.put("ordererName", order.getOrdererName());
		ctx.put("ordererEmail", order.getOrdererEmail());
		ctx.put("orderId", order.getId());

		ctx.put("matchTitle", match.getHomeClub().getKoName() + " vs " + match.getAwayClub().getKoName());
		ctx.put("matchDateTime", formatInstant(match.getMatchAt()));
		ctx.put("stadiumName", match.getStadium().getKoName());
		ctx.put("stadiumAddress", match.getStadium().getAddress());

		ctx.put("seats", seats);

		String paidAt = (payment != null && payment.getPaidAt() != null)
			? formatInstant(payment.getPaidAt())
			: formatInstant(event.getOccurredAt());
		ctx.put("paidAt", paidAt);
		ctx.put("paymentMethod", PAYMENT_METHOD_NAMES.getOrDefault(
			event.getPaymentMethod(), event.getPaymentMethod()));

		ctx.put("totalAmount", order.getTotalAmount());
		ctx.put("bookingFee", order.getBookingFee() != null ? order.getBookingFee() : 0);

		int seatTotal = orderSeats.stream().mapToInt(OrderSeat::getPrice).sum();
		ctx.put("seatTotal", seatTotal);

		ctx.put("cancelDeadline", formatInstant(match.getMatchAt()));

		return Optional.of(ctx);
	}

	@Transactional(readOnly = true)
	public Optional<Map<String, Object>> buildBookingEmailContext(Long orderId) {
		Order order = orderRepository.findByIdWithMatchDetails(orderId).orElse(null);
		if (order == null) {
			return Optional.empty();
		}

		Match match = order.getMatch();
		List<OrderSeat> orderSeats = orderSeatRepository.findByOrderId(orderId);
		List<SeatDisplayInfo> seats = buildSeatDisplayInfos(orderSeats);

		Map<String, Object> ctx = new HashMap<>();
		ctx.put("ordererName", order.getOrdererName());
		ctx.put("ordererEmail", order.getOrdererEmail());
		ctx.put("orderId", order.getId());

		ctx.put("matchTitle", match.getHomeClub().getKoName() + " vs " + match.getAwayClub().getKoName());
		ctx.put("matchDateTime", formatInstant(match.getMatchAt()));
		ctx.put("stadiumName", match.getStadium().getKoName());
		ctx.put("stadiumAddress", match.getStadium().getAddress());

		ctx.put("seats", seats);

		ctx.put("totalAmount", order.getTotalAmount());
		ctx.put("bookingFee", order.getBookingFee() != null ? order.getBookingFee() : 0);
		ctx.put("cancelDeadline", formatInstant(match.getMatchAt()));

		// 입금 기한: 주문일 + 1일 23:59 (KST)
		Instant createdAt = order.getCreatedAt() != null ? order.getCreatedAt() : Instant.now();
		LocalDate orderDate = createdAt.atZone(KST).toLocalDate();
		ZonedDateTime deadline = ZonedDateTime.of(orderDate.plusDays(1), LocalTime.of(23, 59), KST);
		ctx.put("paymentDeadline", deadline.format(DATE_FORMATTER));

		return Optional.of(ctx);
	}

	@Transactional(readOnly = true)
	public Optional<Map<String, Object>> buildCancellationEmailContext(Long orderId, OrderCancelledEvent event) {
		Order order = orderRepository.findByIdWithMatchDetails(orderId).orElse(null);
		if (order == null) {
			return Optional.empty();
		}

		Match match = order.getMatch();
		List<OrderSeat> orderSeats = orderSeatRepository.findByOrderId(orderId);
		List<SeatDisplayInfo> seats = buildSeatDisplayInfos(orderSeats);

		Map<String, Object> ctx = new HashMap<>();
		ctx.put("ordererName", order.getOrdererName());
		ctx.put("ordererEmail", order.getOrdererEmail());
		ctx.put("orderId", order.getId());

		ctx.put("matchTitle", match.getHomeClub().getKoName() + " vs " + match.getAwayClub().getKoName());
		ctx.put("matchDateTime", formatInstant(match.getMatchAt()));
		ctx.put("stadiumName", match.getStadium().getKoName());
		ctx.put("stadiumAddress", match.getStadium().getAddress());

		ctx.put("seats", seats);

		ctx.put("totalAmount", order.getTotalAmount());
		ctx.put("cancellationFee", event.getCancellationFee());
		ctx.put("refundedAmount", event.getRefundedAmount());

		return Optional.of(ctx);
	}

	private List<SeatDisplayInfo> buildSeatDisplayInfos(List<OrderSeat> orderSeats) {
		Set<Long> sectionIds = orderSeats.stream()
			.map(OrderSeat::getSectionId).collect(Collectors.toSet());
		Set<Long> blockIds = orderSeats.stream()
			.map(OrderSeat::getBlockId).collect(Collectors.toSet());

		Map<Long, String> sectionNames = getSectionNames(sectionIds);
		Map<Long, Long> blockNums = getBlockNums(blockIds);

		return orderSeats.stream()
			.map(os -> new SeatDisplayInfo(
				sectionNames.getOrDefault(os.getSectionId(), ""),
				blockNums.getOrDefault(os.getBlockId(), 0L),
				os.getRowNo(),
				os.getSeatNo(),
				os.getPrice()
			))
			.toList();
	}

	private Map<Long, String> getSectionNames(Set<Long> sectionIds) {
		if (sectionIds.isEmpty()) {
			return Map.of();
		}
		String sql = "SELECT id, name FROM sections WHERE id IN (:ids)";
		MapSqlParameterSource params = new MapSqlParameterSource("ids", sectionIds);
		return jdbcTemplate.query(sql, params, rs -> {
			Map<Long, String> map = new HashMap<>();
			while (rs.next()) {
				map.put(rs.getLong("id"), rs.getString("name"));
			}
			return map;
		});
	}

	private Map<Long, Long> getBlockNums(Set<Long> blockIds) {
		if (blockIds.isEmpty()) {
			return Map.of();
		}
		String sql = "SELECT id, block_num FROM blocks WHERE id IN (:ids)";
		MapSqlParameterSource params = new MapSqlParameterSource("ids", blockIds);
		return jdbcTemplate.query(sql, params, rs -> {
			Map<Long, Long> map = new HashMap<>();
			while (rs.next()) {
				map.put(rs.getLong("id"), rs.getLong("block_num"));
			}
			return map;
		});
	}

	public List<Map<String, Object>> buildForcedCancellationEmailContexts(Long userId) {
		Instant now = Instant.now();
		List<OrderStatus> activeStatuses = List.of(OrderStatus.PAID, OrderStatus.PAYMENT_PENDING);
		List<Order> upcomingOrders = orderRepository.findUpcomingOrdersByUserIdAndStatuses(userId, activeStatuses, now);

		return upcomingOrders.stream()
			.map(order -> buildForcedCancellationContext(order, now))
			.toList();
	}

	private Map<String, Object> buildForcedCancellationContext(Order order, Instant cancelledAt) {
		Match match = order.getMatch();
		List<OrderSeat> orderSeats = orderSeatRepository.findByOrderId(order.getId());
		Payment payment = paymentRepository.findByOrderId(order.getId()).orElse(null);
		List<SeatDisplayInfo> seats = buildSeatDisplayInfos(orderSeats);

		int seatTotal = orderSeats.stream().mapToInt(OrderSeat::getPrice).sum();
		int cancellationFee = 0;
		int refundedAmount = order.getTotalAmount() - cancellationFee;

		Map<String, Object> ctx = new HashMap<>();
		ctx.put("ordererName", order.getOrdererName());
		ctx.put("ordererEmail", order.getOrdererEmail());
		ctx.put("orderId", order.getId());

		ctx.put("matchTitle", match.getHomeClub().getKoName() + " vs " + match.getAwayClub().getKoName());
		ctx.put("matchDateTime", formatInstant(match.getMatchAt()));
		ctx.put("stadiumName", match.getStadium().getKoName());
		ctx.put("stadiumAddress", match.getStadium().getAddress());

		ctx.put("seats", seats);

		String paidAt = (payment != null && payment.getPaidAt() != null)
			? formatInstant(payment.getPaidAt())
			: "";
		ctx.put("paidAt", paidAt);
		ctx.put("paymentMethod", payment != null
			? PAYMENT_METHOD_NAMES.getOrDefault(payment.getPaymentMethod().name(), payment.getPaymentMethod().name())
			: "");
		ctx.put("cancelledAt", formatInstant(cancelledAt));

		ctx.put("totalAmount", order.getTotalAmount());
		ctx.put("bookingFee", order.getBookingFee() != null ? order.getBookingFee() : 0);
		ctx.put("seatTotal", seatTotal);
		ctx.put("cancellationFee", cancellationFee);
		ctx.put("refundedAmount", refundedAmount);
		ctx.put("cancelDeadline", formatInstant(match.getMatchAt()));

		return ctx;
	}

	private String formatInstant(Instant instant) {
		if (instant == null) {
			return "";
		}
		return instant.atZone(KST).format(DATE_FORMATTER);
	}
}
