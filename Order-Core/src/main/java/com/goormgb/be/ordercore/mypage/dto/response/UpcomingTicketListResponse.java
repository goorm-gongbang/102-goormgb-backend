package com.goormgb.be.ordercore.mypage.dto.response;

import java.time.Instant;
import java.util.List;

import com.goormgb.be.ordercore.order.enums.OrderStatus;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * 경기 예정 티켓 목록 조회 응답 DTO.
 */
@Schema(description = "경기 예정 티켓 목록 응답")
public record UpcomingTicketListResponse(

		@Schema(description = "경기 예정 티켓 총 개수", example = "3")
		int totalCount,

		PaginationInfo pagination,

		@Schema(description = "경기 예정 티켓 목록 (경기 날짜가 가까운 순)")
		List<UpcomingTicketItem> tickets
) {

	@Schema(description = "페이지네이션 정보")
	public record PaginationInfo(
			@Schema(description = "현재 페이지 번호 (0부터 시작)", example = "0")
			int page,
			@Schema(description = "한 페이지 크기", example = "10")
			int size,
			@Schema(description = "전체 티켓 수", example = "3")
			long totalElements,
			@Schema(description = "전체 페이지 수", example = "1")
			int totalPages,
			@Schema(description = "다음 페이지 존재 여부", example = "false")
			boolean hasNext
	) {
	}

	@Schema(description = "경기 예정 티켓 카드 1건")
	public record UpcomingTicketItem(
			@Schema(description = "티켓(주문) ID — 상세 조회 시 이 값을 ticketId로 사용", example = "58")
			Long ticketId,

			@Schema(description = "경기까지 남은 일수. 0이면 당일(D-Day), 3이면 3일 후(D-3)", example = "3")
			long dDay,

			@Schema(description = "주문 상태 코드", example = "PAID")
			OrderStatus status,

			@Schema(description = "주문 상태 한글 라벨 (화면에 그대로 표시 가능)", example = "결제 완료")
			String statusLabel,

			@Schema(description = "매수 (예매한 좌석 수)", example = "2")
			int seatCount,

			MatchInfo match,

			@Schema(description = "좌석 상세 목록")
			List<SeatInfo> seats,

			TicketActions actions
	) {
	}

	@Schema(description = "경기 정보")
	public record MatchInfo(
			@Schema(description = "경기 ID", example = "181")
			Long matchId,

			@Schema(description = "경기 시작 시간 (UTC)", example = "2026-04-10T05:00:00Z")
			Instant matchAt,

			ClubInfo homeClub,
			ClubInfo awayClub,
			StadiumInfo stadium
	) {
	}

	@Schema(description = "구단 정보")
	public record ClubInfo(
			@Schema(description = "구단 ID", example = "1")
			Long clubId,

			@Schema(description = "구단 한글명", example = "LG 트윈스")
			String koName
	) {
	}

	@Schema(description = "경기장 정보")
	public record StadiumInfo(
			@Schema(description = "경기장 ID", example = "1")
			Long stadiumId,

			@Schema(description = "경기장 한글명", example = "잠실야구장")
			String koName
	) {
	}

	@Schema(description = "좌석 정보 — 구역/블럭/열/번호")
	public record SeatInfo(
			@Schema(description = "구역명 (예: 오렌지석, 블루석)", example = "오렌지석")
			String sectionName,

			@Schema(description = "블럭 코드 (예: 206, 103)", example = "206")
			String blockCode,

			@Schema(description = "열 번호", example = "3")
			int rowNo,

			@Schema(description = "좌석 번호", example = "15")
			int seatNo
	) {
	}

	@Schema(description = "버튼 표시 여부")
	public record TicketActions(
			@Schema(description = "입금하기 버튼 표시 여부 (입금 대기 상태일 때 true)", example = "false")
			boolean canDeposit,

			@Schema(description = "취소하기 버튼 표시 여부 (결제 완료 상태일 때 true)", example = "true")
			boolean canCancel
	) {
		public static TicketActions of(OrderStatus status, Instant matchAt, Instant now) {
			boolean isUpcoming = matchAt.isAfter(now);
			return new TicketActions(
					status == OrderStatus.PAYMENT_PENDING && isUpcoming,
					status == OrderStatus.PAID && isUpcoming
			);
		}
	}
}
