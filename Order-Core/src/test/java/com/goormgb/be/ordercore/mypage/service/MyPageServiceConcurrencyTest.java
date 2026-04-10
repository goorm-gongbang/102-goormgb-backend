package com.goormgb.be.ordercore.mypage.service;

import static org.assertj.core.api.Assertions.*;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import com.goormgb.be.domain.club.entity.Club;
import com.goormgb.be.domain.match.entity.Match;
import com.goormgb.be.domain.match.enums.SaleStatus;
import com.goormgb.be.domain.stadium.entity.Stadium;
import com.goormgb.be.ordercore.mypage.dto.response.MyPageTicketQrResponse;
import com.goormgb.be.ordercore.order.entity.Order;
import com.goormgb.be.ordercore.order.enums.OrderStatus;
import com.goormgb.be.ordercore.qrtoken.repository.QrTokenRepository;
import com.goormgb.be.user.entity.User;

import jakarta.persistence.EntityManager;

@SpringBootTest
@ActiveProfiles("test")
@DisplayName("MyPageTicketService 동시성 통합 테스트")
class MyPageServiceConcurrencyTest {

	@MockitoBean
	private KafkaTemplate<String, Object> kafkaTemplate;

	@Autowired
	private MyPageTicketService myPageTicketService;
	@Autowired
	private QrTokenRepository qrTokenRepository;
	@Autowired
	private PlatformTransactionManager transactionManager;
	@Autowired
	private EntityManager entityManager;

	private TransactionTemplate tx;

	@BeforeEach
	void setUp() {
		tx = new TransactionTemplate(transactionManager);
		ensureSeatTables();
		cleanup();
	}

	@Test
	@DisplayName("동일 ticketId로 동시에 QR 조회 요청 시 토큰은 1개만 생성되고 모두 동일 토큰을 받는다")
	void getTicketEntryQr_concurrentRequests_returnsSingleToken() throws Exception {
		SeedData seed = createSeedData();
		int threadCount = 8;

		ExecutorService executor = Executors.newFixedThreadPool(threadCount);
		CountDownLatch ready = new CountDownLatch(threadCount);
		CountDownLatch start = new CountDownLatch(1);

		List<Callable<MyPageTicketQrResponse>> tasks = new ArrayList<>();
		for (int i = 0; i < threadCount; i++) {
			tasks.add(() -> {
				ready.countDown();
				start.await(5, TimeUnit.SECONDS);
				return myPageTicketService.getTicketEntryQr(seed.userId(), seed.orderId());
			});
		}

		List<Future<MyPageTicketQrResponse>> futures = new ArrayList<>();
		for (Callable<MyPageTicketQrResponse> task : tasks) {
			futures.add(executor.submit(task));
		}

		assertThat(ready.await(5, TimeUnit.SECONDS)).isTrue();
		start.countDown();

		Set<String> returnedTokens = new HashSet<>();
		for (Future<MyPageTicketQrResponse> future : futures) {
			MyPageTicketQrResponse response = future.get(10, TimeUnit.SECONDS);
			assertThat(response.ticketId()).isEqualTo(seed.orderId());
			returnedTokens.add(response.qrToken());
		}

		executor.shutdown();
		assertThat(executor.awaitTermination(10, TimeUnit.SECONDS)).isTrue();

		assertThat(returnedTokens).hasSize(1);
		assertThat(qrTokenRepository.findAll()).hasSize(1);
	}

	private void cleanup() {
		tx.executeWithoutResult(status -> {
			entityManager.createNativeQuery("DELETE FROM qr_tokens").executeUpdate();
			entityManager.createNativeQuery("DELETE FROM order_seats").executeUpdate();
			entityManager.createNativeQuery("DELETE FROM orders").executeUpdate();
			entityManager.createNativeQuery("DELETE FROM matches").executeUpdate();
			entityManager.createNativeQuery("DELETE FROM clubs").executeUpdate();
			entityManager.createNativeQuery("DELETE FROM stadiums").executeUpdate();
			entityManager.createNativeQuery("DELETE FROM user_sns").executeUpdate();
			entityManager.createNativeQuery("DELETE FROM users").executeUpdate();
		});
	}

	private void ensureSeatTables() {
		tx.executeWithoutResult(status -> {
			entityManager.createNativeQuery("""
				CREATE TABLE IF NOT EXISTS sections (
					id BIGINT PRIMARY KEY,
					name VARCHAR(255) NOT NULL
				)
				""").executeUpdate();

			entityManager.createNativeQuery("""
				CREATE TABLE IF NOT EXISTS blocks (
					id BIGINT PRIMARY KEY,
					block_code VARCHAR(255) NOT NULL
				)
				""").executeUpdate();
		});
	}

	private SeedData createSeedData() {
		return tx.execute(status -> {
			Stadium stadium = Stadium.builder()
				.region("서울")
				.koName("잠실야구장")
				.enName("Jamsil Baseball Stadium")
				.address("서울특별시 송파구 올림픽로 19-2")
				.build();
			entityManager.persist(stadium);

			Club home = Club.builder()
				.koName("LG 트윈스")
				.enName("LG Twins")
				.clubColor("#C60C30")
				.stadium(stadium)
				.build();
			entityManager.persist(home);

			Club away = Club.builder()
				.koName("두산 베어스")
				.enName("Doosan Bears")
				.clubColor("#131230")
				.stadium(stadium)
				.build();
			entityManager.persist(away);

			Match match = Match.builder()
				.matchAt(Instant.now().plus(1, ChronoUnit.HOURS))
				.homeClub(home)
				.awayClub(away)
				.stadium(stadium)
				.saleStatus(SaleStatus.ON_SALE)
				.build();
			entityManager.persist(match);

			User user = User.builder()
				.email("concurrency@test.com")
				.nickname("concurrency-user")
				.build();
			entityManager.persist(user);

			Order order = Order.builder()
				.userId(user.getId())
				.matchId(match.getId())
				.totalAmount(42000)
				.ordererName("홍길동")
				.ordererEmail("hong@test.com")
				.ordererPhone("010-1234-5678")
				.ordererBirthDate("990831")
				.matchTitle(home.getKoName() + " vs " + away.getKoName())
				.matchDate(match.getMatchAt())
				.stadiumName(stadium.getKoName())
				.homeClubName(home.getKoName())
				.awayClubName(away.getKoName())
				.build();
			order.updateStatus(OrderStatus.PAID);
			entityManager.persist(order);

			entityManager.flush();
			return new SeedData(user.getId(), order.getId());
		});
	}

	private record SeedData(Long userId, Long orderId) {
	}
}
