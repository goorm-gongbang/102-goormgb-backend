package com.goormgb.be.seat.config;

import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.goormgb.be.domain.onboarding.enums.Viewpoint;
import com.goormgb.be.seat.area.entity.Area;
import com.goormgb.be.seat.area.enums.AreaCode;
import com.goormgb.be.seat.area.repository.AreaRepository;
import com.goormgb.be.seat.block.entity.Block;
import com.goormgb.be.seat.block.repository.BlockRepository;
import com.goormgb.be.seat.seat.entity.Seat;
import com.goormgb.be.seat.seat.enums.SeatZone;
import com.goormgb.be.seat.seat.repository.SeatRepository;
import com.goormgb.be.seat.section.entity.Section;
import com.goormgb.be.seat.section.enums.SectionCode;
import com.goormgb.be.seat.section.repository.SectionRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 잠실야구장 정적 좌석 구조 시드 데이터.
 * areas → sections → blocks 순서로 삽입하며, 이미 데이터가 존재하면 스킵합니다.
 */
@Slf4j
@Component
@Order(1)
@RequiredArgsConstructor
public class SeatDataInitializer implements CommandLineRunner {

	private final AreaRepository areaRepository;
	private final SectionRepository sectionRepository;
	private final BlockRepository blockRepository;
	private final SeatRepository seatRepository;

	@Override
	@Transactional
	public void run(String... args) {
		if (blockRepository.count() > 0) {
			log.info("[SeatDataInitializer] 블럭 데이터 이미 존재 - 시드 스킵");
			return;
		}

		log.info("[SeatDataInitializer] 잠실야구장 좌석 구조 시드 데이터 삽입 시작");

		// ── 1. Areas ──
		Area home = saveArea(AreaCode.HOME, "1루(홈)");
		Area away = saveArea(AreaCode.AWAY, "3루(어웨이)");
		Area outfield = saveArea(AreaCode.OUTFIELD, "외야");
		Area center = saveArea(AreaCode.CENTER, "중앙");

		// ── 2. Sections ──
		// 1루(홈) 섹션
		Section homePurple = saveSection(home, SectionCode.PURPLE, "퍼플석(테이블석)");
		Section homeExciting = saveSection(home, SectionCode.EXCITING, "익사이팅존");
		Section homeBlue = saveSection(home, SectionCode.BLUE, "블루석");
		Section homeOrange = saveSection(home, SectionCode.ORANGE, "오렌지석");
		Section homeRed = saveSection(home, SectionCode.RED, "레드석");
		Section homeNavy = saveSection(home, SectionCode.NAVY, "네이비석");

		// 3루(어웨이) 섹션
		Section awayPurple = saveSection(away, SectionCode.PURPLE, "퍼플석(테이블석)");
		Section awayExciting = saveSection(away, SectionCode.EXCITING, "익사이팅존");
		Section awayBlue = saveSection(away, SectionCode.BLUE, "블루석");
		Section awayOrange = saveSection(away, SectionCode.ORANGE, "오렌지석");
		Section awayRed = saveSection(away, SectionCode.RED, "레드석");
		Section awayNavy = saveSection(away, SectionCode.NAVY, "네이비석");

		// 외야 섹션
		Section green = saveSection(outfield, SectionCode.GREEN, "그린석(외야석)");

		// 중앙 섹션
		Section premium = saveSection(center, SectionCode.PREMIUM, "테라존(중앙 프리미엄석)");

		// ── 3. Blocks ──

		// ─── 중앙 프리미엄석 ───
		saveBlock(center, premium, "CP", Viewpoint.CENTER, 50, 50);

		// ─── 익사이팅존 ───
		saveBlock(home, homeExciting, "EX-1", Viewpoint.INFIELD_1B, 30, 40);
		saveBlock(away, awayExciting, "EX-3", Viewpoint.INFIELD_3B, 40, 30);

		// ─── 1루(홈) 오렌지석 (응원석) 205~208 ───
		int rank = 1;
		for (int i = 205; i <= 208; i++) {
			saveBlock(home, homeOrange, String.valueOf(i),
				Viewpoint.INFIELD_1B, rank++, 80 + (i - 205));
		}

		// ─── 3루(어웨이) 오렌지석 (응원석) 219~222 ───
		rank = 1;
		for (int i = 219; i <= 222; i++) {
			saveBlock(away, awayOrange, String.valueOf(i),
				Viewpoint.INFIELD_3B, 80 + (i - 219), rank++);
		}

		// ─── 1루(홈) 퍼플석 (테이블석) 110~113 ───
		for (int i = 110; i <= 113; i++) {
			saveBlock(home, homePurple, String.valueOf(i),
				Viewpoint.INFIELD_1B, 10 + (i - 110), 60 + (i - 110));
		}

		// ─── 3루(어웨이) 퍼플석 (테이블석) 212~215 ───
		for (int i = 212; i <= 215; i++) {
			saveBlock(away, awayPurple, String.valueOf(i),
				Viewpoint.INFIELD_3B, 60 + (i - 212), 10 + (i - 212));
		}

		// ─── 1루(홈) 블루석 114~116 ───
		for (int i = 114; i <= 116; i++) {
			saveBlock(home, homeBlue, String.valueOf(i),
				Viewpoint.INFIELD_1B, 14 + (i - 114), 55 + (i - 114));
		}

		// ─── 1루(홈) 블루석 216~218 (2층) ───
		for (int i = 216; i <= 218; i++) {
			saveBlock(home, homeBlue, String.valueOf(i),
				Viewpoint.INFIELD_1B, 5 + (i - 216), 70 + (i - 216));
		}

		// ─── 3루(어웨이) 블루석 107~109 ───
		for (int i = 107; i <= 109; i++) {
			saveBlock(away, awayBlue, String.valueOf(i),
				Viewpoint.INFIELD_3B, 55 + (i - 107), 14 + (i - 107));
		}

		// ─── 3루(어웨이) 블루석 209~211 (2층) ───
		for (int i = 209; i <= 211; i++) {
			saveBlock(away, awayBlue, String.valueOf(i),
				Viewpoint.INFIELD_3B, 70 + (i - 209), 5 + (i - 209));
		}

		// ─── 1루(홈) 레드석 117~122 ───
		for (int i = 117; i <= 122; i++) {
			saveBlock(home, homeRed, String.valueOf(i),
				Viewpoint.INFIELD_1B, 17 + (i - 117), 45 + (i - 117));
		}

		// ─── 1루(홈) 레드석 223~226 (2층) ───
		for (int i = 223; i <= 226; i++) {
			saveBlock(home, homeRed, String.valueOf(i),
				Viewpoint.INFIELD_1B, 8 + (i - 223), 65 + (i - 223));
		}

		// ─── 3루(어웨이) 레드석 101~106 ───
		for (int i = 101; i <= 106; i++) {
			saveBlock(away, awayRed, String.valueOf(i),
				Viewpoint.INFIELD_3B, 45 + (i - 101), 17 + (i - 101));
		}

		// ─── 3루(어웨이) 레드석 201~204 (2층) ───
		for (int i = 201; i <= 204; i++) {
			saveBlock(away, awayRed, String.valueOf(i),
				Viewpoint.INFIELD_3B, 65 + (i - 201), 8 + (i - 201));
		}

		// ─── 1루(홈) 네이비석 301~317 ───
		for (int i = 301; i <= 317; i++) {
			saveBlock(home, homeNavy, String.valueOf(i),
				Viewpoint.INFIELD_1B, 23 + (i - 301), 35 + (i - 301));
		}

		// ─── 3루(어웨이) 네이비석 318~334 ───
		for (int i = 318; i <= 334; i++) {
			saveBlock(away, awayNavy, String.valueOf(i),
				Viewpoint.INFIELD_3B, 35 + (i - 318), 23 + (i - 318));
		}

		// ─── 외야 그린석 401~407 (1루 방향 → OUTFIELD_R) ───
		for (int i = 401; i <= 407; i++) {
			saveBlock(outfield, green, String.valueOf(i),
				Viewpoint.OUTFIELD_R, 60 + (i - 401), 90 + (i - 401));
		}

		// ─── 외야 그린석 408~415 (중앙 → OUTFIELD_C) ───
		for (int i = 408; i <= 415; i++) {
			saveBlock(outfield, green, String.valueOf(i),
				Viewpoint.OUTFIELD_C, 70 + (i - 408), 70 + (i - 408));
		}

		// ─── 외야 그린석 416~422 (3루 방향 → OUTFIELD_L) ───
		for (int i = 416; i <= 422; i++) {
			saveBlock(outfield, green, String.valueOf(i),
				Viewpoint.OUTFIELD_L, 90 + (i - 416), 60 + (i - 416));
		}

		log.info("[SeatDataInitializer] 시드 데이터 삽입 완료 - 블럭 {}개", blockRepository.count());
	}

	private Area saveArea(AreaCode code, String name) {
		return areaRepository.save(Area.builder()
			.code(code)
			.name(name)
			.build());
	}

	private Section saveSection(Area area, SectionCode code, String name) {
		return sectionRepository.save(Section.builder()
			.area(area)
			.code(code)
			.name(name)
			.build());
	}

	private void saveBlock(Area area, Section section,
		String blockCode, Viewpoint viewpoint, Integer homeCheerRank, Integer awayCheerRank) {
		Block block = blockRepository.save(Block.builder()
			.area(area)
			.section(section)
			.blockCode(blockCode)
			.viewpoint(viewpoint)
			.homeCheerRank(homeCheerRank)
			.awayCheerRank(awayCheerRank)
			.build());

		saveSeats(block);
	}

	private void saveSeats(Block block) {
		for (RowPattern pattern : SeatLayoutPatterns.STANDARD) {
			saveRow(
				block,
				pattern.rowNo(),
				pattern.seatCount(),
				pattern.startTemplateColNo(),
				resolveSeatZone(pattern.rowNo())
			);
		}
	}

	private SeatZone resolveSeatZone(int rowNo) {
		if (rowNo <= 3) {
			return SeatZone.LOW;
		}
		if (rowNo <= 7) {
			return SeatZone.MID;
		}
		return SeatZone.HIGH;
	}

	private void saveRow(
		Block block,
		int rowNo,
		int seatCount,
		int startTemplateColNo,
		SeatZone seatZone
	) {
		for (int i = 0; i < seatCount; i++) {
			seatRepository.save(
				Seat.builder()
					.block(block)
					.rowNo(rowNo)
					.seatNo(i + 1)
					.templateColNo(startTemplateColNo + i)
					.seatZone(seatZone)
					.build()
			);
		}
	}
}
