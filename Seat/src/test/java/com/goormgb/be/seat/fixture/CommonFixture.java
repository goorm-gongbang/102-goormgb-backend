package com.goormgb.be.seat.fixture;

import java.time.Instant;

import org.springframework.test.util.ReflectionTestUtils;

import com.goormgb.be.domain.club.entity.Club;
import com.goormgb.be.domain.match.entity.Match;
import com.goormgb.be.domain.match.enums.SaleStatus;
import com.goormgb.be.domain.stadium.entity.Stadium;
import com.goormgb.be.user.entity.User;

public final class CommonFixture {

	private CommonFixture() {
	}

	public static Club club(Long id, String name) {
		Club club = Club.builder()
			.koName(name).enName(name).logoImg("logo.png").clubColor("#000").build();
		ReflectionTestUtils.setField(club, "id", id);
		return club;
	}

	public static Club lgClub() {
		return club(1L, "LG 트윈스");
	}

	public static Club doosanClub() {
		return club(2L, "두산 베어스");
	}

	public static Club samsungClub() {
		return club(3L, "삼성 라이온즈");
	}

	public static User user(Long id) {
		User user = User.builder().build();
		ReflectionTestUtils.setField(user, "id", id);
		return user;
	}

	public static Stadium jamsil() {
		return Stadium.builder()
			.region("서울").koName("잠실").enName("Jamsil").address("서울시").build();
	}

	public static Match match(Club home, Club away) {
		return Match.create(Instant.now(), home, away, jamsil(), SaleStatus.ON_SALE);
	}
}
