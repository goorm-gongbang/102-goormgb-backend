package com.goormgb.be.ordercore.mypage.service;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
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
import com.goormgb.be.ordercore.mypage.dto.response.UpcomingTicketListResponse;
import com.goormgb.be.ordercore.mypage.enums.TicketTab;
import com.goormgb.be.ordercore.mypage.query.MyPageQueryService;
import com.goormgb.be.ordercore.mypage.query.MyPageQueryService.OrderSeatRow;
import com.goormgb.be.ordercore.mypage.query.MyPageQueryService.TicketRow;
import com.goormgb.be.ordercore.mypage.query.MyPageQueryService.UpcomingTicketRow;
import com.goormgb.be.ordercore.mypage.service.support.MyPageTicketCancellationCalculator;
import com.goormgb.be.ordercore.mypage.service.support.MyPageTicketDetailAssembler;
import com.goormgb.be.ordercore.mypage.service.support.MyPageTicketListAssembler;
import com.goormgb.be.ordercore.mypage.service.support.MyPageTicketQrSupport;
import com.goormgb.be.ordercore.order.entity.Order;
import com.goormgb.be.ordercore.order.enums.OrderStatus;
import com.goormgb.be.ordercore.order.event.OrderEventPublisher;
import com.goormgb.be.ordercore.order.repository.OrderMyPageSummaryCounts;
import com.goormgb.be.ordercore.order.repository.OrderRepository;
import com.goormgb.be.ordercore.order.repository.OrderSeatRepository;
import com.goormgb.be.ordercore.payment.entity.Payment;
import com.goormgb.be.ordercore.payment.enums.PaymentMethod;
import com.goormgb.be.ordercore.payment.repository.PaymentRepository;
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

	private static final ZoneId KST = ZoneId.of("Asia/Seoul");

	private static final List<OrderStatus> UPCOMING_STATUSES = List.of(
			OrderStatus.PAYMENT_PENDING,
			OrderStatus.PAID,
			OrderStatus.UNDER_REVIEW
	);

	private static final List<String> UPCOMING_TICKET_STATUSES = List.of(
			OrderStatus.PAYMENT_PENDING.name(),
			OrderStatus.PAID.name(),
			OrderStatus.UNDER_REVIEW.name()
	);

	private static final List<OrderStatus> CANCEL_PROCESSING_STATUSES = List.of(
			OrderStatus.CANCEL_REQUESTED,
			OrderStatus.REFUND_PROCESSING
	);

	private final OrderRepository orderRepository;
	private final OrderSeatRepository orderSeatRepository;
	private final QrTokenRepository qrTokenRepository;
	private final PaymentRepository paymentRepository;
	private final MyPageQueryService myPageQueryService;
	private final CancellationFeePolicyRepository cancellationFeePolicyRepository;
	private final OrderEventPublisher orderEventPublisher;
	private final Clock clock;

	public MyPageTicketListResponse getTickets(Long userId, String tab, int page, int size) {
		Preconditions.validate(size <= MAX_PAGE_SIZE, ErrorCode.INVALID_PAGE_SIZE);

		TicketTab ticketTab = TicketTab.fromString(tab);
		List<String> statusNames = ticketTab.getStatusNames();

		Instant now = Instant.now(clock);
		OrderMyPageSummaryCounts counts = orderRepository.findMyPageSummaryCounts(
				userId,
				UPCOMING_STATUSES,
				CANCEL_PROCESSING_STATUSES,
				CANCEL_PROCESSING_STATUSES,
				OrderStatus.PAID,
				now
		);
		int totalCount = (int)counts.totalCount();
		int upcomingCount = (int)counts.upcomingCount();
		int cancelProcessingCount = (int)counts.cancelProcessingCount();
		int completedCount = (int)counts.completedCount();

		long totalElements = myPageQueryService.countTickets(userId, statusNames);
		List<TicketRow> ticketRows = myPageQueryService.findTickets(userId, statusNames, page, size);

		List<MyPageTicketListResponse.TicketItem> tickets;
		if (ticketRows.isEmpty()) {
			tickets = List.of();
		} else {
			List<Long> orderIds = ticketRows.stream().map(TicketRow::orderId).toList();
			List<OrderSeatRow> seatRows = myPageQueryService.findOrderSeatRowsByOrderIds(orderIds);
			Map<Long, List<MyPageTicketListResponse.SeatInfo>> seatMap = MyPageTicketListAssembler.toSeatMap(seatRows);
			tickets = MyPageTicketListAssembler.toTicketItems(ticketRows, seatMap);
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

	/**
	 * 경기 예정 티켓 목록을 조회한다. (오늘 이후 경기 + 유효 상태만)
	 */
	public UpcomingTicketListResponse getUpcomingTickets(Long userId, int page, int size) {
		Preconditions.validate(page >= 0 && size > 0 && size <= MAX_PAGE_SIZE, ErrorCode.INVALID_PAGE_SIZE);

		Instant now = Instant.now(clock);
		LocalDate today = now.atZone(KST).toLocalDate();

		long totalElements = myPageQueryService.countUpcomingTickets(userId, UPCOMING_TICKET_STATUSES, now);
		List<UpcomingTicketRow> rows = myPageQueryService.findUpcomingTickets(
				userId, UPCOMING_TICKET_STATUSES, now, page, size);

		List<UpcomingTicketListResponse.UpcomingTicketItem> tickets;
		if (rows.isEmpty()) {
			tickets = List.of();
		} else {
			List<Long> orderIds = rows.stream().map(UpcomingTicketRow::orderId).toList();
			List<OrderSeatRow> seatRows = myPageQueryService.findOrderSeatRowsByOrderIds(orderIds);
			Map<Long, List<UpcomingTicketListResponse.SeatInfo>> seatMap = seatRows.stream()
					.collect(Collectors.groupingBy(
							OrderSeatRow::orderId,
							Collectors.mapping(
									row -> new UpcomingTicketListResponse.SeatInfo(
											row.sectionName(), row.blockCode(), row.rowNo(), row.seatNo()),
									Collectors.toList())));

			tickets = rows.stream()
					.map(row -> {
						long dDay = ChronoUnit.DAYS.between(today,
								row.matchAt().atZone(KST).toLocalDate());
						return new UpcomingTicketListResponse.UpcomingTicketItem(
								row.orderId(),
								dDay,
								row.status(),
								row.status().getDescription(),
								row.seatCount(),
								new UpcomingTicketListResponse.MatchInfo(
										row.matchId(),
										row.matchAt(),
										new UpcomingTicketListResponse.ClubInfo(row.homeClubId(), row.homeClubName()),
										new UpcomingTicketListResponse.ClubInfo(row.awayClubId(), row.awayClubName()),
										new UpcomingTicketListResponse.StadiumInfo(row.stadiumId(), row.stadiumName())),
								seatMap.getOrDefault(row.orderId(), List.of()),
								UpcomingTicketListResponse.TicketActions.of(row.status(), row.matchAt(), now));
					})
					.toList();
		}

		int totalPages = totalElements == 0 ? 0 : (int)Math.ceil((double)totalElements / size);
		boolean hasNext = (long)(page + 1) * size < totalElements;

		return new UpcomingTicketListResponse(
				(int)totalElements,
				new UpcomingTicketListResponse.PaginationInfo(page, size, totalElements, totalPages, hasNext),
				tickets);
	}

	public MyPageTicketDetailResponse getTicketDetail(Long userId, Long ticketId) {
		TicketDetailBaseRow base = myPageQueryService.findTicketDetailBaseByOrderId(ticketId)
				.orElseThrow(() -> new CustomException(ErrorCode.ORDER_NOT_FOUND));
		Preconditions.validate(base.userId().equals(userId), ErrorCode.ORDER_ACCESS_DENIED);
		List<TicketSeatDetailRow> seatRows = myPageQueryService.findTicketSeatRowsByOrderId(ticketId);

		MyPageTicketDetailResponse.PaymentInfo payment = MyPageTicketDetailAssembler.toPaymentInfo(base);

		MyPageTicketDetailResponse.CancellationPolicy cancellationPolicy =
				MyPageTicketDetailAssembler.buildCancellationPolicy(base.matchAt(), clock,
						cancellationFeePolicyRepository);

		MyPageTicketDetailResponse.VirtualAccount virtualAccount = MyPageTicketDetailAssembler.toVirtualAccount(base);
		MyPageTicketDetailResponse.CancellationInfo cancellation = MyPageTicketDetailAssembler.toCancellation(base);

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
		MyPageTicketQrSupport.validateQrIssuableTime(order.getMatch().getMatchAt(), now);

		QrToken qrToken = qrTokenRepository.findByOrderIdAndExpiresAtAfter(ticketId, now)
				.orElseGet(() -> MyPageTicketQrSupport.issueNewQrToken(order, now, qrTokenRepository));

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

		CancellationFeePolicy policy = MyPageTicketCancellationCalculator.findCancellationPolicy(
				order.getMatch().getMatchAt(),
				now,
				cancellationFeePolicyRepository
		);
		Preconditions.validate(Boolean.TRUE.equals(policy.getCancellable()), ErrorCode.TICKET_CANCEL_NOT_ALLOWED);

		int cancellationFee = MyPageTicketCancellationCalculator.calculateCancellationFee(order, policy, now);
		int refundedAmount = order.getTotalAmount() - cancellationFee;

		Payment payment = paymentRepository.findByOrderId(ticketId)
				.orElseThrow(() -> new CustomException(ErrorCode.PAYMENT_NOT_FOUND));

		if (payment.getPaymentMethod() == PaymentMethod.BANK_TRANSFER) {
			order.refundComplete(cancellationFee, refundedAmount, now);
			payment.refund();
		} else {
			order.cancelComplete(cancellationFee, refundedAmount, now);
			payment.cancel();
		}

		// 주문 취소 이벤트 발행 → Seat 서비스에서 좌석 SOLD → AVAILABLE 복원
		List<Long> matchSeatIds = orderSeatRepository.findMatchSeatIdsByOrderId(ticketId);
		orderEventPublisher.publishOrderCancelled(order, matchSeatIds);
		orderSeatRepository.deleteByOrderId(ticketId);

		return MyPageTicketCancelResponse.of(order);
	}
}
