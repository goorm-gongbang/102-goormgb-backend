package com.goormgb.be.seat.internal.controller;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.goormgb.be.seat.internal.dto.PriceResponse;
import com.goormgb.be.seat.internal.dto.SeatHoldInfoResponse;
import com.goormgb.be.seat.internal.dto.SectionBlockInfoResponse;
import com.goormgb.be.seat.internal.service.SeatInternalService;

import lombok.RequiredArgsConstructor;

/**
 * Order-Core 모듈 전용 내부 API.
 * DB 스키마 분리 시 Order-Core → Seat 직접 쿼리를 대체한다.
 */
@RestController
@RequestMapping("/internal")
@RequiredArgsConstructor
public class SeatInternalController {

	private final SeatInternalService seatInternalService;

	/**
	 * 좌석 선점 정보를 조회한다.
	 */
	@GetMapping("/seat-holds")
	public ResponseEntity<List<SeatHoldInfoResponse>> getSeatHoldInfos(
		@RequestParam Long userId,
		@RequestParam Long matchId,
		@RequestParam List<Long> matchSeatIds
	) {
		return ResponseEntity.ok(seatInternalService.findSeatHoldInfos(userId, matchId, matchSeatIds));
	}

	/**
	 * 구역/요일/좌석유형 조합의 가격을 조회한다.
	 */
	@GetMapping("/price-policies")
	public ResponseEntity<PriceResponse> getPrice(
		@RequestParam Long sectionId,
		@RequestParam String dayType,
		@RequestParam String ticketType
	) {
		return ResponseEntity.ok(seatInternalService.findPrice(sectionId, dayType, ticketType));
	}

	/**
	 * 구역/블럭 이름 정보를 조회한다.
	 */
	@GetMapping("/section-blocks")
	public ResponseEntity<List<SectionBlockInfoResponse>> getSectionBlockInfos(
		@RequestParam List<Long> sectionIds,
		@RequestParam List<Long> blockIds
	) {
		return ResponseEntity.ok(seatInternalService.findSectionBlockInfos(sectionIds, blockIds));
	}
}
