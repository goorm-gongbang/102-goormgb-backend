package com.goormgb.be.seat.config;

import java.util.ArrayList;
import java.util.List;

import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.goormgb.be.seat.pricePolicy.entity.PricePolicy;
import com.goormgb.be.seat.pricePolicy.enums.DayType;
import com.goormgb.be.domain.ticket.enums.TicketType;
import com.goormgb.be.seat.pricePolicy.repository.PricePolicyRepository;
import com.goormgb.be.seat.section.entity.Section;
import com.goormgb.be.seat.section.enums.SectionCode;
import com.goormgb.be.seat.section.repository.SectionRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 잠실야구장 입장권 가격 시드 데이터.
 * SeatDataInitializer(@Order(1)) 이후 실행되어 sections를 참조함.
 *
 * 가격 정책 (불변):
 *   - 중앙석(PREMIUM)  : 80,000 / 80,000
 *   - 테이블석(PURPLE)  : 52,000 / 58,000
 *   - 익사이팅존(EXCITING): 28,000 / 33,000
 *   - 블루석(BLUE)     : 22,000 / 24,000  (장애인 50% 할인)
 *   - 오렌지석(ORANGE)  : 20,000 / 22,000  (장애인 50% 할인)
 *   - 레드석(RED)      : 17,000 / 19,000  (장애인 50% 할인)
 *   - 네이비석(NAVY)   : 14,000 / 14,000  (장애인 50% 할인)
 *   - 외야(GREEN)      : 일반 9,000/10,000 | 청소년·군경 7,000/8,000 | 어린이·유공자·경로·장애인 4,500/5,000
 *
 * 주말 기준: 금요일, 토요일, 일요일, 공휴일
 */
@Slf4j
@Component
@Order(2)
@RequiredArgsConstructor
public class PricePolicyInitializer implements CommandLineRunner {

	private static final Long JAMSIL_STADIUM_ID = 1L;

	private final SectionRepository sectionRepository;
	private final PricePolicyRepository pricePolicyRepository;

	@Override
	@Transactional
	public void run(String... args) {
		if (pricePolicyRepository.count() > 0) {
			log.info("[PricePolicyInitializer] 가격 정책 데이터 이미 존재 - 시드 스킵");
			return;
		}

		log.info("[PricePolicyInitializer] 잠실야구장 입장권 가격 시드 데이터 삽입 시작");

		List<Section> sections = sectionRepository.findAll();
		List<PricePolicy> policies = new ArrayList<>();

		for (Section section : sections) {
			policies.addAll(buildPolicies(section));
		}

		pricePolicyRepository.saveAll(policies);
		log.info("[PricePolicyInitializer] 가격 정책 시드 데이터 삽입 완료 - {}건", policies.size());
	}

	private List<PricePolicy> buildPolicies(Section section) {
		Long sectionId = section.getId();
		SectionCode code = section.getCode();
		List<PricePolicy> result = new ArrayList<>();

		switch (code) {
			case PREMIUM -> {
				// 중앙석: 80,000 (주중=주말 동일)
				result.add(policy(sectionId, DayType.WEEKDAY, TicketType.ADULT, 80_000));
				result.add(policy(sectionId, DayType.WEEKEND, TicketType.ADULT, 80_000));
			}
			case PURPLE -> {
				// 테이블석: 52,000 / 58,000
				result.add(policy(sectionId, DayType.WEEKDAY, TicketType.ADULT, 52_000));
				result.add(policy(sectionId, DayType.WEEKEND, TicketType.ADULT, 58_000));
			}
			case EXCITING -> {
				// 익사이팅존: 28,000 / 33,000
				result.add(policy(sectionId, DayType.WEEKDAY, TicketType.ADULT, 28_000));
				result.add(policy(sectionId, DayType.WEEKEND, TicketType.ADULT, 33_000));
			}
			case BLUE -> {
				// 블루석: 22,000 / 24,000 | 장애인 50%: 11,000 / 12,000
				result.add(policy(sectionId, DayType.WEEKDAY, TicketType.ADULT, 22_000));
				result.add(policy(sectionId, DayType.WEEKEND, TicketType.ADULT, 24_000));
				result.add(policy(sectionId, DayType.WEEKDAY, TicketType.DISABLED, 11_000));
				result.add(policy(sectionId, DayType.WEEKEND, TicketType.DISABLED, 12_000));
			}
			case ORANGE -> {
				// 오렌지석: 20,000 / 22,000 | 장애인 50%: 10,000 / 11,000
				result.add(policy(sectionId, DayType.WEEKDAY, TicketType.ADULT, 20_000));
				result.add(policy(sectionId, DayType.WEEKEND, TicketType.ADULT, 22_000));
				result.add(policy(sectionId, DayType.WEEKDAY, TicketType.DISABLED, 10_000));
				result.add(policy(sectionId, DayType.WEEKEND, TicketType.DISABLED, 11_000));
			}
			case RED -> {
				// 레드석: 17,000 / 19,000 | 장애인 50%: 8,500 / 9,500
				result.add(policy(sectionId, DayType.WEEKDAY, TicketType.ADULT, 17_000));
				result.add(policy(sectionId, DayType.WEEKEND, TicketType.ADULT, 19_000));
				result.add(policy(sectionId, DayType.WEEKDAY, TicketType.DISABLED, 8_500));
				result.add(policy(sectionId, DayType.WEEKEND, TicketType.DISABLED, 9_500));
			}
			case NAVY -> {
				// 네이비석: 14,000 (주중=주말) | 장애인 50%: 7,000
				result.add(policy(sectionId, DayType.WEEKDAY, TicketType.ADULT, 14_000));
				result.add(policy(sectionId, DayType.WEEKEND, TicketType.ADULT, 14_000));
				result.add(policy(sectionId, DayType.WEEKDAY, TicketType.DISABLED, 7_000));
				result.add(policy(sectionId, DayType.WEEKEND, TicketType.DISABLED, 7_000));
			}
			case GREEN -> {
				// 외야 지정석: 일반 / 청소년·군경 / 어린이·유공자·경로·장애인
				result.add(policy(sectionId, DayType.WEEKDAY, TicketType.ADULT, 9_000));
				result.add(policy(sectionId, DayType.WEEKEND, TicketType.ADULT, 10_000));
				result.add(policy(sectionId, DayType.WEEKDAY, TicketType.YOUTH, 7_000));
				result.add(policy(sectionId, DayType.WEEKEND, TicketType.YOUTH, 8_000));
				result.add(policy(sectionId, DayType.WEEKDAY, TicketType.MILITARY, 7_000));
				result.add(policy(sectionId, DayType.WEEKEND, TicketType.MILITARY, 8_000));
				result.add(policy(sectionId, DayType.WEEKDAY, TicketType.CHILD, 4_500));
				result.add(policy(sectionId, DayType.WEEKEND, TicketType.CHILD, 5_000));
				result.add(policy(sectionId, DayType.WEEKDAY, TicketType.VETERAN, 4_500));
				result.add(policy(sectionId, DayType.WEEKEND, TicketType.VETERAN, 5_000));
				result.add(policy(sectionId, DayType.WEEKDAY, TicketType.SENIOR, 4_500));
				result.add(policy(sectionId, DayType.WEEKEND, TicketType.SENIOR, 5_000));
				result.add(policy(sectionId, DayType.WEEKDAY, TicketType.DISABLED, 4_500));
				result.add(policy(sectionId, DayType.WEEKEND, TicketType.DISABLED, 5_000));
			}
			default -> log.warn("[PricePolicyInitializer] 가격 정책 미정의 섹션 코드: {} (section_id={}). 가격 정책을 추가해 주세요.", code, sectionId);
		}

		return result;
	}

	private PricePolicy policy(Long sectionId, DayType dayType, TicketType ticketType, int price) {
		return PricePolicy.builder()
			.stadiumId(JAMSIL_STADIUM_ID)
			.sectionId(sectionId)
			.dayType(dayType)
			.ticketType(ticketType)
			.price(price)
			.build();
	}
}
