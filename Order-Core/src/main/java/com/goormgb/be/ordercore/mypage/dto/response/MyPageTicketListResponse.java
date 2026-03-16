package com.goormgb.be.ordercore.mypage.dto.response;

import java.time.Instant;
import java.util.List;

import com.goormgb.be.ordercore.order.enums.OrderStatus;

public record MyPageTicketListResponse(
	TicketSummary summary,
	String currentTab,
	PaginationInfo pagination,
	List<TicketItem> tickets
) {

	public record TicketSummary(
		int totalCount,
		int upcomingCount,
		int cancelProcessingCount,
		int completedCount
	) {
	}

	public record PaginationInfo(
		int page,
		int size,
		long totalElements,
		int totalPages,
		boolean hasNext
	) {
	}

	public record TicketItem(
		Long ticketId,
		Instant matchAt,
		ClubInfo homeClub,
		ClubInfo awayClub,
		String stadiumName,
		int seatCount,
		List<SeatInfo> seats,
		OrderStatus status,
		TicketActions actions
	) {
	}

	public record ClubInfo(
		Long clubId,
		String koName
	) {
	}

	public record SeatInfo(
		String sectionName,
		String blockCode,
		int rowNo,
		int seatNo
	) {
	}

	public record TicketActions(
		boolean canDeposit,
		boolean canCancel,
		boolean canViewDetail
	) {
		public static TicketActions of(OrderStatus status, Instant matchAt) {
			boolean isUpcoming = matchAt.isAfter(Instant.now());
			return new TicketActions(
				status == OrderStatus.PAYMENT_PENDING && isUpcoming,
				status == OrderStatus.PAID && isUpcoming,
				true
			);
		}
	}

	public static MyPageTicketListResponse of(
		int totalCount,
		int upcomingCount,
		int cancelProcessingCount,
		int completedCount,
		String currentTab,
		int page,
		int size,
		long totalElements,
		int totalPages,
		boolean hasNext,
		List<TicketItem> tickets
	) {
		return new MyPageTicketListResponse(
			new TicketSummary(totalCount, upcomingCount, cancelProcessingCount, completedCount),
			currentTab,
			new PaginationInfo(page, size, totalElements, totalPages, hasNext),
			tickets
		);
	}
}
