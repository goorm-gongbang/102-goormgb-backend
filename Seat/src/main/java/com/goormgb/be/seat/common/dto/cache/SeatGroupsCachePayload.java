package com.goormgb.be.seat.common.dto.cache;

import java.io.Serializable;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.goormgb.be.seat.common.dto.response.SeatGroupsEntryResponse;

/**
 * {@code GET /matches/{matchId}/seat-groups} 응답 중 <b>유저에 독립적인</b> 부분을 담는 Redis 캐시 payload.
 *
 * <p>원본 응답은 {@link SeatGroupsEntryResponse} 이며, 그 중
 * {@code match}(MatchInfo) 와 {@code seatGroups}(List&lt;SeatGroupInfo&gt;) 는 matchId 가 같으면
 * 동일한 값을 가진다. 반면 {@code seatSession} 은 유저별 booking-options 에서 파생되므로 캐시 대상에서 제외한다.</p>
 *
 * <p>캐시 HIT 시 본 payload 와 유저별 {@link SeatGroupsEntryResponse.SeatSessionInfo} 를 결합해
 * 최종 응답을 조립한다. TTL 5초 이내 잔여 좌석 수 stale 은 UX 상 수용한다.</p>
 */
public record SeatGroupsCachePayload(
	SeatGroupsEntryResponse.MatchInfo match,
	List<SeatGroupsEntryResponse.SeatGroupInfo> seatGroups
) implements Serializable {

	@JsonCreator
	public SeatGroupsCachePayload(
		@JsonProperty("match") SeatGroupsEntryResponse.MatchInfo match,
		@JsonProperty("seatGroups") List<SeatGroupsEntryResponse.SeatGroupInfo> seatGroups
	) {
		this.match = match;
		this.seatGroups = seatGroups;
	}
}
