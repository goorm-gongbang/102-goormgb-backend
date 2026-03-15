package com.goormgb.be.ordercore.mypage.service;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.goormgb.be.global.exception.ErrorCode;
import com.goormgb.be.global.support.Preconditions;
import com.goormgb.be.ordercore.mypage.dto.response.MyPageProfileResponse;
import com.goormgb.be.ordercore.mypage.dto.response.MyPageTicketListResponse;
import com.goormgb.be.ordercore.mypage.enums.TicketTab;
import com.goormgb.be.ordercore.mypage.query.MyPageQueryService;
import com.goormgb.be.ordercore.mypage.query.MyPageQueryService.OrderSeatRow;
import com.goormgb.be.ordercore.mypage.query.MyPageQueryService.TicketRow;
import com.goormgb.be.ordercore.order.enums.OrderStatus;
import com.goormgb.be.ordercore.order.repository.OrderRepository;
import com.goormgb.be.user.entity.User;
import com.goormgb.be.user.entity.UserSns;
import com.goormgb.be.user.repository.UserRepository;
import com.goormgb.be.user.repository.UserSnsRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MyPageService {

	private static final int MAX_PAGE_SIZE = 10;

	private static final List<OrderStatus> UPCOMING_STATUSES = List.of(
		OrderStatus.PAYMENT_PENDING,
		OrderStatus.PAID
	);

	private static final List<OrderStatus> CANCEL_REFUND_STATUSES = List.of(
		OrderStatus.CANCEL_REQUESTED,
		OrderStatus.CANCELLED,
		OrderStatus.REFUND_PROCESSING,
		OrderStatus.REFUND_COMPLETED
	);

	private static final List<OrderStatus> CANCEL_PROCESSING_STATUSES = List.of(
		OrderStatus.CANCEL_REQUESTED,
		OrderStatus.REFUND_PROCESSING
	);

	private final UserRepository userRepository;
	private final UserSnsRepository userSnsRepository;
	private final OrderRepository orderRepository;
	private final MyPageQueryService myPageQueryService;

	/**
	 * 마이페이지 프로필 요약 조회
	 */
	public MyPageProfileResponse getProfile(Long userId) {
		User user = userRepository.findByIdOrThrow(userId, ErrorCode.USER_NOT_FOUND);
		UserSns userSns = userSnsRepository.findByUserId(userId).orElse(null);

		Instant now = Instant.now();
		long upcomingCount = orderRepository.countUpcomingOrders(userId, UPCOMING_STATUSES, now);
		long cancelRefundCount = orderRepository.countByUserIdAndStatusIn(userId, CANCEL_REFUND_STATUSES);
		long completedCount = orderRepository.countCompletedOrders(userId, now);

		log.info("[MyPageService] 프로필 조회 - userId={}, upcomingCount={}, cancelRefundCount={}, completedCount={}",
				userId, upcomingCount, cancelRefundCount, completedCount);

		return MyPageProfileResponse.of(user, userSns, upcomingCount, cancelRefundCount, completedCount);
	}

	/**
	 * 마이페이지 예매 내역 목록 조회
	 */
	public MyPageTicketListResponse getTickets(Long userId, String tab, int page, int size) {
		Preconditions.validate(size <= MAX_PAGE_SIZE, ErrorCode.INVALID_PAGE_SIZE);

		TicketTab ticketTab = TicketTab.fromString(tab);
		List<String> statusNames = ticketTab.getStatusNames();

		Instant now = Instant.now();
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

		log.info("[MyPageService] 예매 내역 조회 - userId={}, tab={}, page={}, size={}, totalElements={}",
				userId, tab, page, size, totalElements);

		return MyPageTicketListResponse.of(
				totalCount, upcomingCount, cancelProcessingCount, completedCount,
				ticketTab.name(), page, size, totalElements, totalPages, hasNext, tickets
		);
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
}
