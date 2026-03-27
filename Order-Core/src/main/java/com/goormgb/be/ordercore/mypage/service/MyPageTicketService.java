package com.goormgb.be.ordercore.mypage.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.goormgb.be.global.exception.CustomException;
import com.goormgb.be.global.exception.ErrorCode;
import com.goormgb.be.global.support.Preconditions;
import com.goormgb.be.ordercore.cancellation.entity.CancellationFeePolicy;
import com.goormgb.be.ordercore.cancellation.repository.CancellationFeePolicyRepository;
import com.goormgb.be.ordercore.mypage.dto.query.TicketDetailBaseRow;
import com.goormgb.be.ordercore.mypage.dto.query.TicketSeatDetailRow;
import com.goormgb.be.ordercore.mypage.dto.response.MyPageTicketCancelResponse;
import com.goormgb.be.ordercore.mypage.dto.response.MyPageTicketDetailResponse;
import com.goormgb.be.ordercore.mypage.dto.response.MyPageTicketListResponse;
import com.goormgb.be.ordercore.mypage.dto.response.MyPageTicketQrResponse;
import com.goormgb.be.ordercore.mypage.enums.TicketTab;
import com.goormgb.be.ordercore.mypage.query.MyPageQueryService;
import com.goormgb.be.ordercore.mypage.query.MyPageQueryService.OrderSeatRow;
import com.goormgb.be.ordercore.mypage.query.MyPageQueryService.TicketRow;
import com.goormgb.be.ordercore.order.entity.Order;
import com.goormgb.be.ordercore.order.enums.OrderStatus;
import com.goormgb.be.ordercore.order.repository.OrderRepository;
import com.goormgb.be.ordercore.payment.enums.PaymentMethod;
import com.goormgb.be.ordercore.qrtoken.entity.QrToken;
import com.goormgb.be.ordercore.qrtoken.repository.QrTokenRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MyPageTicketService {

	private static final int MAX_PAGE_SIZE = 10;
	private static final long QR_REFRESH_INTERVAL_SECONDS = 180L;
	private static final Duration ENTRY_OPEN_BEFORE_MATCH = Duration.ofHours(3);
	private static final ZoneId KST = ZoneId.of("Asia/Seoul");

	private static final List<OrderStatus> UPCOMING_STATUSES = List.of(
		OrderStatus.PAYMENT_PENDING,
		OrderStatus.PAID
	);

	private static final List<OrderStatus> CANCEL_PROCESSING_STATUSES = List.of(
		OrderStatus.CANCEL_REQUESTED,
		OrderStatus.REFUND_PROCESSING
	);

	private final OrderRepository orderRepository;
	private final QrTokenRepository qrTokenRepository;
	private final MyPageQueryService myPageQueryService;
	private final CancellationFeePolicyRepository cancellationFeePolicyRepository;
	private final Clock clock;

	public MyPageTicketListResponse getTickets(Long userId, String tab, int page, int size) {
		Preconditions.validate(size <= MAX_PAGE_SIZE, ErrorCode.INVALID_PAGE_SIZE);

		TicketTab ticketTab = TicketTab.fromString(tab);
		List<String> statusNames = ticketTab.getStatusNames();

		Instant now = Instant.now(clock);
		int totalCount = (int)orderRepository.countByUserId(userId);
		int upcomingCount = (int)orderRepository.countUpcomingOrders(userId, UPCOMING_STATUSES, now);
		int cancelProcessingCount = (int)orderRepository.countByUserIdAndStatusIn(userId, CANCEL_PROCESSING_STATUSES);
		int completedCount = (int)orderRepository.countCompletedOrders(userId, now);

		long totalElements = myPageQueryService.countTickets(userId, statusNames);
		List<TicketRow> ticketRows = myPageQueryService.findTickets(userId, statusNames, page, size);

		List<MyPageTicketListResponse.TicketItem> tickets;
		if (ticketRows.isEmpty()) {
			tickets = List.of();
		} else {
			List<Long> orderIds = ticketRows.stream().map(TicketRow::orderId).toList();
			Map<Long, List<MyPageTicketListResponse.SeatInfo>> seatMap = buildSeatMap(orderIds);
			tickets = ticketRows.stream()
				.map(row -> toTicketItem(row, seatMap.getOrDefault(row.orderId(), List.of())))
				.toList();
		}

		int totalPages = totalElements == 0 ? 0 : (int)Math.ceil((double)totalElements / size);
		boolean hasNext = (long)(page + 1) * size < totalElements;

		log.info("[MyPageTicketService] 예매 내역 조회 - userId={}, tab={}, page={}, size={}, totalElements={}",
			userId, tab, page, size, totalElements);

		return MyPageTicketListResponse.of(
			totalCount, upcomingCount, cancelProcessingCount, completedCount,
			ticketTab.name(), page, size, totalElements, totalPages, hasNext, tickets
		);
	}

	public MyPageTicketDetailResponse getTicketDetail(Long userId, Long ticketId) {
		TicketDetailBaseRow base = myPageQueryService.findTicketDetailBaseByOrderId(ticketId)
			.orElseThrow(() -> new CustomException(ErrorCode.ORDER_NOT_FOUND));
		Preconditions.validate(base.userId().equals(userId), ErrorCode.ORDER_ACCESS_DENIED);
		List<TicketSeatDetailRow> seatRows = myPageQueryService.findTicketSeatRowsByOrderId(ticketId);

		MyPageTicketDetailResponse.PaymentInfo payment = new MyPageTicketDetailResponse.PaymentInfo(
			base.totalAmount(),
			base.bookingFee(),
			toPaymentMethodValue(base.paymentMethod()),
			base.paidAt(),
			toCashReceipt(base)
		);

		MyPageTicketDetailResponse.CancellationPolicy cancellationPolicy =
			buildCancellationPolicy(base.matchAt());

		MyPageTicketDetailResponse.VirtualAccount virtualAccount = toVirtualAccount(base);
		MyPageTicketDetailResponse.CancellationInfo cancellation = toCancellation(base);

		return MyPageTicketDetailResponse.of(
			base,
			seatRows,
			payment,
			cancellationPolicy,
			virtualAccount,
			cancellation
		);
	}

	@Transactional
	public MyPageTicketQrResponse getTicketEntryQr(Long userId, Long ticketId) {
		Order order = orderRepository.findByIdForUpdate(ticketId)
			.orElseThrow(() -> new CustomException(ErrorCode.ORDER_NOT_FOUND));
		Preconditions.validate(order.getUser().getId().equals(userId), ErrorCode.ORDER_ACCESS_DENIED);
		Preconditions.validate(order.getStatus() == OrderStatus.PAID, ErrorCode.INVALID_ORDER_STATUS);

		Instant now = Instant.now(clock);
		validateQrIssuableTime(order.getMatch().getMatchAt(), now);

		QrToken qrToken = qrTokenRepository.findByOrderIdAndExpiresAtAfter(ticketId, now)
			.orElseGet(() -> issueNewQrToken(order, now));

		List<TicketSeatDetailRow> seatRows = myPageQueryService.findTicketSeatRowsByOrderId(ticketId);
		return MyPageTicketQrResponse.of(order, seatRows, qrToken.getQrToken(), qrToken.getExpiresAt());
	}

	@Transactional
	public MyPageTicketCancelResponse requestTicketCancel(Long userId, Long ticketId) {
		Instant now = Instant.now(clock);
		Order order = orderRepository.findByIdForUpdate(ticketId)
			.orElseThrow(() -> new CustomException(ErrorCode.ORDER_NOT_FOUND));
		Preconditions.validate(order.getUser().getId().equals(userId), ErrorCode.ORDER_ACCESS_DENIED);
		Preconditions.validate(order.getStatus() == OrderStatus.PAID, ErrorCode.INVALID_ORDER_STATUS);

		CancellationFeePolicy policy = findCancellationPolicy(order.getMatch().getMatchAt(), now);
		Preconditions.validate(Boolean.TRUE.equals(policy.getCancellable()), ErrorCode.TICKET_CANCEL_NOT_ALLOWED);

		int cancellationFee = calculateCancellationFee(order, policy, now);
		int refundedAmount = order.getTotalAmount() - cancellationFee;
		order.cancel(cancellationFee, refundedAmount);

		return MyPageTicketCancelResponse.of(order);
	}

	private Map<Long, List<MyPageTicketListResponse.SeatInfo>> buildSeatMap(List<Long> orderIds) {
		List<OrderSeatRow> seatRows = myPageQueryService.findOrderSeatRowsByOrderIds(orderIds);
		return seatRows.stream()
			.collect(Collectors.groupingBy(
				OrderSeatRow::orderId,
				Collectors.mapping(
					row -> new MyPageTicketListResponse.SeatInfo(
						row.sectionName(), row.blockCode(), row.rowNo(), row.seatNo()
					),
					Collectors.toList()
				)
			));
	}

	private MyPageTicketListResponse.TicketItem toTicketItem(
		TicketRow row,
		List<MyPageTicketListResponse.SeatInfo> seats
	) {
		return new MyPageTicketListResponse.TicketItem(
			row.orderId(),
			row.matchAt(),
			new MyPageTicketListResponse.ClubInfo(row.homeClubId(), row.homeClubName()),
			new MyPageTicketListResponse.ClubInfo(row.awayClubId(), row.awayClubName()),
			row.stadiumName(),
			row.seatCount(),
			seats,
			row.status(),
			MyPageTicketListResponse.TicketActions.of(row.status(), row.matchAt())
		);
	}

	private MyPageTicketDetailResponse.VirtualAccount toVirtualAccount(TicketDetailBaseRow row) {
		if (row.paymentMethod() != PaymentMethod.BANK_TRANSFER || row.status() != OrderStatus.PAYMENT_PENDING) {
			return null;
		}

		return new MyPageTicketDetailResponse.VirtualAccount(
			row.accountBank(),
			row.accountNumber(),
			row.accountHolder(),
			row.depositDeadline()
		);
	}

	private MyPageTicketDetailResponse.CancellationInfo toCancellation(TicketDetailBaseRow row) {
		if (row.cancelledAt() == null) {
			return null;
		}

		return new MyPageTicketDetailResponse.CancellationInfo(
			row.cancelledAt(),
			row.cancellationFee(),
			row.refundedAmount()
		);
	}

	private MyPageTicketDetailResponse.CashReceiptInfo toCashReceipt(TicketDetailBaseRow row) {
		if (row.cashReceiptPurpose() == null || row.cashReceiptNumber() == null) {
			return null;
		}

		return new MyPageTicketDetailResponse.CashReceiptInfo(
			row.cashReceiptPurpose().name(),
			row.cashReceiptNumber(),
			row.totalAmount()
		);
	}

	private MyPageTicketDetailResponse.CancellationPolicy buildCancellationPolicy(Instant matchAt) {
		Instant deadline = matchAt.atZone(KST)
			.minusDays(1)
			.withHour(23)
			.withMinute(59)
			.withSecond(0)
			.withNano(0)
			.toInstant();

		int daysLeft = Math.max(0, (int)ChronoUnit.DAYS.between(
			Instant.now(clock).atZone(KST).toLocalDate(),
			matchAt.atZone(KST).toLocalDate()
		));

		CancellationFeePolicy policy = cancellationFeePolicyRepository.findByDaysLeft(daysLeft)
			.orElseThrow(() -> new CustomException(ErrorCode.INTERNAL_SERVER_ERROR));

		return new MyPageTicketDetailResponse.CancellationPolicy(
			deadline,
			toPercentString(policy.getTicketFeeRate())
		);
	}

	private String toPercentString(BigDecimal feeRate) {
		BigDecimal percent = feeRate.multiply(BigDecimal.valueOf(100));
		return percent.setScale(3, RoundingMode.DOWN).stripTrailingZeros().toPlainString() + "%";
	}

	private String toPaymentMethodValue(PaymentMethod method) {
		if (method == null) {
			return null;
		}
		if (method == PaymentMethod.BANK_TRANSFER) {
			return "VIRTUAL_ACCOUNT";
		}
		return method.name();
	}

	private void validateQrIssuableTime(Instant matchAt, Instant now) {
		Preconditions.validate(now.isBefore(matchAt), ErrorCode.ENTRY_QR_MATCH_STARTED);
		Preconditions.validate(!now.isBefore(matchAt.minus(ENTRY_OPEN_BEFORE_MATCH)),
			ErrorCode.ENTRY_QR_NOT_AVAILABLE_YET);
	}

	private QrToken issueNewQrToken(Order order, Instant now) {
		QrToken qrToken = QrToken.builder()
			.order(order)
			.user(order.getUser())
			.qrToken(UUID.randomUUID().toString())
			.expiresAt(calculateNextQrExpiry(now))
			.build();
		return qrTokenRepository.save(qrToken);
	}

	private Instant calculateNextQrExpiry(Instant now) {
		long nowEpochSec = now.getEpochSecond();
		long expiresEpochSec = ((nowEpochSec / QR_REFRESH_INTERVAL_SECONDS) + 1) * QR_REFRESH_INTERVAL_SECONDS;
		return Instant.ofEpochSecond(expiresEpochSec);
	}

	private CancellationFeePolicy findCancellationPolicy(Instant matchAt, Instant now) {
		int daysLeft = Math.max(0, (int)ChronoUnit.DAYS.between(
			now.atZone(KST).toLocalDate(),
			matchAt.atZone(KST).toLocalDate()
		));
		return cancellationFeePolicyRepository.findByDaysLeft(daysLeft)
			.orElseThrow(() -> new CustomException(ErrorCode.INTERNAL_SERVER_ERROR));
	}

	private int calculateCancellationFee(Order order, CancellationFeePolicy policy, Instant now) {
		int ticketAmount = Math.max(0, order.getTotalAmount() - order.getBookingFee());
		int ticketFee = policy.getTicketFeeRate()
			.multiply(BigDecimal.valueOf(ticketAmount))
			.setScale(0, RoundingMode.DOWN)
			.intValue();

		if (Boolean.TRUE.equals(policy.getBookingFeeRefundable()) && isSameBookingDate(order, now)) {
			return ticketFee;
		}
		return ticketFee + order.getBookingFee();
	}

	private boolean isSameBookingDate(Order order, Instant now) {
		if (order.getCreatedAt() == null) {
			return false;
		}
		LocalDate bookingDate = order.getCreatedAt().atZone(KST).toLocalDate();
		LocalDate cancelDate = now.atZone(KST).toLocalDate();
		return bookingDate.equals(cancelDate);
	}
}
