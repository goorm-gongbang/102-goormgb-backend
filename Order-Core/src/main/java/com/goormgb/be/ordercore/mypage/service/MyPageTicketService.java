package com.goormgb.be.ordercore.mypage.service;

import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.Map;

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
import com.goormgb.be.ordercore.mypage.service.support.MyPageTicketCancellationCalculator;
import com.goormgb.be.ordercore.mypage.service.support.MyPageTicketDetailAssembler;
import com.goormgb.be.ordercore.mypage.service.support.MyPageTicketListAssembler;
import com.goormgb.be.ordercore.mypage.service.support.MyPageTicketQrSupport;
import com.goormgb.be.ordercore.order.entity.Order;
import com.goormgb.be.ordercore.order.enums.OrderStatus;
import com.goormgb.be.ordercore.order.query.SeatInfoQueryService;
import com.goormgb.be.ordercore.order.repository.OrderMyPageSummaryCounts;
import com.goormgb.be.ordercore.order.repository.OrderRepository;
import com.goormgb.be.ordercore.order.repository.OrderSeatRepository;
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

	private static final List<OrderStatus> UPCOMING_STATUSES = List.of(
			OrderStatus.PAYMENT_PENDING,
			OrderStatus.PAID
	);

	private static final List<OrderStatus> CANCEL_PROCESSING_STATUSES = List.of(
			OrderStatus.CANCEL_REQUESTED,
			OrderStatus.REFUND_PROCESSING
	);

	private final OrderRepository orderRepository;
	private final OrderSeatRepository orderSeatRepository;
	private final QrTokenRepository qrTokenRepository;
	private final MyPageQueryService myPageQueryService;
	private final CancellationFeePolicyRepository cancellationFeePolicyRepository;
	private final SeatInfoQueryService seatInfoQueryService;
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
		order.cancel(cancellationFee, refundedAmount);

		// 결제된 좌석을 SOLD → AVAILABLE로 복원
		List<Long> matchSeatIds = orderSeatRepository.findMatchSeatIdsByOrderId(ticketId);
		int restored = seatInfoQueryService.markAvailableIfSold(matchSeatIds);
		log.info("[MyPageTicketService] 좌석 AVAILABLE 복원 - orderId={}, count={}", ticketId, restored);

		return MyPageTicketCancelResponse.of(order);
	}
}
