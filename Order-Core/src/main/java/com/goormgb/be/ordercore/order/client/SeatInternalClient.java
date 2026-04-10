package com.goormgb.be.ordercore.order.client;

import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import com.goormgb.be.ordercore.order.query.SeatHoldInfo;

import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;

/**
 * Seat 모듈의 내부 API를 호출하는 클라이언트.
 * DB 스키마 분리 후 Order-Core가 Seat DB를 직접 조회하지 않도록 대체한다.
 */
@Slf4j
@Component
public class SeatInternalClient {

	@Value("${seat.internal.base-url:http://localhost:8082/seat}")
	private String seatBaseUrl;

	private RestClient restClient;

	@PostConstruct
	void init() {
		this.restClient = RestClient.builder()
			.baseUrl(seatBaseUrl)
			.build();
	}

	/**
	 * 좌석 선점 정보를 조회한다.
	 */
	public List<SeatHoldInfo> findSeatHoldInfos(Long userId, Long matchId, List<Long> matchSeatIds) {
		String matchSeatIdsParam = matchSeatIds.stream()
			.map(String::valueOf)
			.reduce((a, b) -> a + "," + b)
			.orElse("");

		return restClient.get()
			.uri("/internal/seat-holds?userId={userId}&matchId={matchId}&matchSeatIds={matchSeatIds}",
				userId, matchId, matchSeatIdsParam)
			.retrieve()
			.body(new ParameterizedTypeReference<>() {});
	}

	/**
	 * 구역/요일/좌석유형 조합의 가격을 조회한다.
	 */
	public Integer findPrice(Long sectionId, String dayType, String ticketType) {
		PriceResponse response = restClient.get()
			.uri("/internal/price-policies?sectionId={sectionId}&dayType={dayType}&ticketType={ticketType}",
				sectionId, dayType, ticketType)
			.retrieve()
			.body(PriceResponse.class);

		return response != null ? response.price() : null;
	}

	/**
	 * 구역/블럭 이름 정보를 조회한다.
	 */
	public List<SectionBlockInfo> findSectionBlockInfos(List<Long> sectionIds, List<Long> blockIds) {
		String sectionIdsParam = sectionIds.stream().map(String::valueOf).reduce((a, b) -> a + "," + b).orElse("");
		String blockIdsParam = blockIds.stream().map(String::valueOf).reduce((a, b) -> a + "," + b).orElse("");

		return restClient.get()
			.uri("/internal/section-blocks?sectionIds={sectionIds}&blockIds={blockIds}",
				sectionIdsParam, blockIdsParam)
			.retrieve()
			.body(new ParameterizedTypeReference<>() {});
	}

	public record PriceResponse(Integer price) {}

	public record SectionBlockInfo(Long sectionId, String sectionName, Long blockId, String blockCode) {}
}
