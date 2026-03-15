package com.goormgb.be.seat.common.service;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.BDDMockito.*;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import com.goormgb.be.domain.match.repository.MatchRepository;
import com.goormgb.be.seat.area.entity.Area;
import com.goormgb.be.seat.area.enums.AreaCode;
import com.goormgb.be.seat.block.entity.Block;
import com.goormgb.be.seat.block.repository.BlockRepository;
import com.goormgb.be.seat.common.dto.response.SectionBlocksResponse;
import com.goormgb.be.seat.matchSeat.entity.MatchSeat;
import com.goormgb.be.seat.matchSeat.enums.MatchSeatSaleStatus;
import com.goormgb.be.seat.matchSeat.repository.MatchSeatRepository;
import com.goormgb.be.seat.redis.SeatPreferenceRedisRepository;
import com.goormgb.be.seat.seat.enums.SeatZone;
import com.goormgb.be.seat.seatHold.entity.SeatHold;
import com.goormgb.be.seat.seatHold.repository.SeatHoldRepository;
import com.goormgb.be.seat.section.entity.Section;
import com.goormgb.be.seat.section.enums.SectionCode;
import com.goormgb.be.seat.section.repository.SectionRepository;

@ExtendWith(MockitoExtension.class)
class SeatCommonServiceTest {

	@Mock
	private MatchRepository matchRepository;
	@Mock
	private SeatPreferenceRedisRepository seatPreferenceRedisRepository;
	@Mock
	private SectionRepository sectionRepository;
	@Mock
	private BlockRepository blockRepository;
	@Mock
	private MatchSeatRepository matchSeatRepository;
	@Mock
	private SeatHoldRepository seatHoldRepository;

	@InjectMocks
	private SeatCommonService seatCommonService;

	private Section createSection(Long sectionId) {
		Area area = Area.builder().code(AreaCode.HOME).name("1루 구역").build();
		ReflectionTestUtils.setField(area, "id", 11L);
		Section section = Section.builder().area(area).code(SectionCode.ORANGE).name("오렌지석").build();
		ReflectionTestUtils.setField(section, "id", sectionId);
		return section;
	}

	private Block createBlock(Long blockId, String blockCode, Section section) {
		Block block = Block.builder()
			.area(section.getArea())
			.section(section)
			.blockCode(blockCode)
			.viewpoint(com.goormgb.be.domain.onboarding.enums.Viewpoint.INFIELD_1B)
			.homeCheerRank(1)
			.awayCheerRank(1)
			.build();
		ReflectionTestUtils.setField(block, "id", blockId);
		return block;
	}

	private MatchSeat createMatchSeat(
		Long id,
		Long matchId,
		Long seatId,
		Long sectionId,
		Long blockId,
		int rowNo,
		int seatNo,
		MatchSeatSaleStatus saleStatus
	) {
		MatchSeat seat = MatchSeat.builder()
			.matchId(matchId)
			.seatId(seatId)
			.areaId(11L)
			.sectionId(sectionId)
			.blockId(blockId)
			.rowNo(rowNo)
			.seatNo(seatNo)
			.templateColNo(seatNo)
			.seatZone(SeatZone.MID)
			.saleStatus(saleStatus)
			.build();
		ReflectionTestUtils.setField(seat, "id", id);
		return seat;
	}

	private SeatHold createSeatHold(Long matchSeatId, Long matchId, Instant expiresAt) {
		return SeatHold.builder()
			.matchSeatId(matchSeatId)
			.matchId(matchId)
			.seatId(999L)
			.userId(77L)
			.expiresAt(expiresAt)
			.build();
	}

	@Test
	@DisplayName("섹션 블럭 좌석 현황 조회 시 HELD 좌석을 반영하고 행별 남은 좌석 수를 계산한다")
	void 섹션_블럭_좌석_현황_HELD_반영_성공() {
		Long userId = 1L;
		Long matchId = 10L;
		Long sectionId = 20L;

		Section section = createSection(sectionId);
		Block block205 = createBlock(205L, "205", section);
		Block block206 = createBlock(206L, "206", section);

		MatchSeat s1 = createMatchSeat(1001L, matchId, 205001L, sectionId, 205L, 1, 1, MatchSeatSaleStatus.AVAILABLE);
		MatchSeat s2 = createMatchSeat(1002L, matchId, 205002L, sectionId, 205L, 1, 2, MatchSeatSaleStatus.SOLD);
		MatchSeat s3 = createMatchSeat(1003L, matchId, 205003L, sectionId, 205L, 1, 3, MatchSeatSaleStatus.AVAILABLE);
		MatchSeat s4 = createMatchSeat(1004L, matchId, 206001L, sectionId, 206L, 2, 1, MatchSeatSaleStatus.BLOCKED);

		lenient().when(sectionRepository.findById(sectionId)).thenReturn(Optional.of(section));
		lenient().when(blockRepository.findBySectionIdOrderByBlockCodeAsc(sectionId))
			.thenReturn(List.of(block205, block206));
		lenient().when(
				matchSeatRepository.findByMatchIdAndSectionIdOrderByBlockIdAscRowNoAscSeatNoAsc(matchId, sectionId))
			.thenReturn(List.of(s1, s2, s3, s4));
		lenient().when(seatHoldRepository.findAllByMatchIdAndExpiresAtAfter(eq(matchId), any(Instant.class)))
			.thenReturn(List.of(
				createSeatHold(1003L, matchId, Instant.now().plusSeconds(60)),
				createSeatHold(9999L, matchId, Instant.now().plusSeconds(60))
			));

		SectionBlocksResponse result = seatCommonService.getSectionBlocks(matchId, sectionId, userId);

		assertThat(result.blocks()).hasSize(2);
		assertThat(result.blocks().get(0).blockCode()).isEqualTo("205");
		assertThat(result.blocks().get(0).displayName()).isEqualTo("205블럭");
		assertThat(result.blocks().get(0).rows()).hasSize(1);
		assertThat(result.blocks().get(0).rows().get(0).remainingSeatCount()).isEqualTo(1);
		assertThat(result.blocks().get(0).rows().get(0).seats())
			.extracting(SectionBlocksResponse.SeatInfo::saleStatus)
			.containsExactly("AVAILABLE", "SOLD", "HELD");
		assertThat(result.blocks().get(1).rows()).hasSize(1);
		assertThat(result.blocks().get(1).rows().get(0).remainingSeatCount()).isZero();
	}
}