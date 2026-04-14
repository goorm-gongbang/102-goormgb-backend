# Seat BookingOptions API Match 조회 Caffeine 캐싱 적용 작업계획서

- **지라 티켓**: GRGB-XXX (추천 이름: `Seat BookingOptions API Match 조회 Caffeine 로컬 캐시 적용`)
- **브랜치**: `perf/seat-booking-options-match-cache`
- **작성일**: 2026-04-15
- **관련 트러블슈팅**: `docs/트러블슈팅/Seat-부하테스트-P99-6887ms-Match조회-캐싱미적용-트러블슈팅.md`

---

## 1. 배경

1000 VU 부하테스트에서 `POST /seat/api/v1/matches/{matchId}/booking-options` API의 **P99 지연시간이 6887ms**까지 치솟는 현상이 관측되었다.

원인은 `BookingOptionsService.saveBookingOptions` 내부의 **`matchRepository.findByIdOrThrow(matchId)` 호출이 매 요청마다 PostgreSQL에 `SELECT match WHERE id = ?` 쿼리를 발생시키는 것**이었다. HikariCP 풀(20개)과 동시 요청(최대 Tomcat 200 스레드) 사이에서 burst 구간에 커넥션 경합이 발생하며 tail latency가 폭증했다.

해당 API는 Match 엔티티를 **존재 검증 용도로만** 사용하며, 필드 값을 전혀 읽지 않는다. Match는 거의 불변 데이터이므로 로컬 캐싱이 최적 대상이다.

---

## 2. 현재 상태 (AS-IS)

### 2-1. 코드

```java
// Seat/src/main/java/com/goormgb/be/seat/booking/service/BookingOptionsService.java:31
public BookingOptionsResponse saveBookingOptions(Long matchId, Long userId, BookingOptionsRequest request) {
    matchRepository.findByIdOrThrow(matchId, ErrorCode.MATCH_NOT_FOUND);
    // ... 이후 Redis 작업
}
```

### 2-2. 지표 (2026-04-15 부하테스트)

| 지표 | Queue | **Seat** |
|-----|-------|----------|
| Avg | 1487ms | **1999ms** |
| P95 | 2023ms | **6372ms** |
| P99 | 2231ms | **6887ms** |
| 성공률 | 100% | 100% |

### 2-3. 구성

| 항목 | 값 |
|-----|---|
| `server.tomcat.threads.max` | 200 |
| `hikari.maximum-pool-size` | 20 |
| Match 조회 캐시 | 없음 (요청당 1회 DB 조회) |

---

## 3. 변경 상태 (TO-BE)

### 3-1. 최종 흐름

```
[요청] → MatchExistenceValidator.validateExists(matchId)
          ↓ (Caffeine cache hit, sub-ms)
       검증 통과 → Redis 작업
```

### 3-2. 기대 효과 (추정)

| 지표 | 현재 | 예상 |
|-----|-----|-----|
| 요청당 DB 쿼리 | 1회 | 0회 (캐시 hit 시) |
| HikariCP 점유 시간 | 5~30ms | 0ms |
| **P99** | 6887ms | 1800~3000ms (56~74% ↓) |
| **P95** | 6372ms | 1500~2500ms (60~76% ↓) |

---

## 4. 상세 설계

### 4-1. 의존성 추가 (`seat/build.gradle`)

```gradle
implementation 'org.springframework.boot:spring-boot-starter-cache'
implementation 'com.github.ben-manes.caffeine:caffeine'
```

> Spring Boot BOM이 Caffeine 버전을 관리하므로 버전 명시 불필요.

### 4-2. `CacheConfig` 신규 생성

경로: `seat/src/main/java/com/goormgb/be/seat/config/CacheConfig.java`

- `@EnableCaching` 선언
- Caffeine `CacheManager` 빈 등록
- 캐시명 `match-exists`: `maximumSize=1000, expireAfterWrite=10m`

### 4-3. `MatchExistenceValidator` 신규 생성

경로: `seat/src/main/java/com/goormgb/be/seat/booking/service/MatchExistenceValidator.java`

- `matchRepository.existsById(matchId)` 를 래핑
- `@Cacheable(cacheNames = "match-exists", key = "#matchId", unless = "!#result")`
  - `unless` 조건으로 **false(존재하지 않음)는 캐싱하지 않음** → 존재하지 않는 matchId로 반복 공격이 들어와도 DB 부하가 소량 증가할 뿐, 정상 트래픽의 매치가 생성된 직후 즉시 캐시 반영 가능
- `validateExists(Long matchId)` 메서드로 존재하지 않으면 `MATCH_NOT_FOUND` 예외

### 4-4. `BookingOptionsService` 수정

- `MatchRepository` 의존성 제거
- `MatchExistenceValidator` 주입
- `matchRepository.findByIdOrThrow(matchId, ...)` → `matchExistenceValidator.validateExists(matchId)`

### 4-5. `application.yaml` 설정 추가

```yaml
spring:
  cache:
    type: caffeine
    cache-names: match-exists
    caffeine:
      spec: maximumSize=1000,expireAfterWrite=10m,recordStats
```

---

## 5. 영향 범위

| 대상 | 영향 |
|-----|-----|
| `BookingOptionsService.saveBookingOptions` | 내부 호출만 변경 (API 응답 동일) |
| 다른 서비스(Queue, Order-Core) | 영향 없음 (common-core의 `MatchRepository` 미변경) |
| 다른 Seat API (`SeatCommonService.getSectionBlocks` 등) | 영향 없음 (별도 메서드 `findDetailByIdOrThrow` 사용) |
| DB 스키마 | 변경 없음 |
| 테스트 | `BookingOptionsService` 테스트의 mock 대상 변경 필요 |

---

## 6. 수정 파일 목록

| # | 파일 | 변경 |
|---|-----|-----|
| 1 | `seat/build.gradle` | cache + caffeine 의존성 추가 |
| 2 | `seat/src/main/java/com/goormgb/be/seat/config/CacheConfig.java` | 신규 생성 |
| 3 | `seat/src/main/java/com/goormgb/be/seat/booking/service/MatchExistenceValidator.java` | 신규 생성 |
| 4 | `seat/src/main/java/com/goormgb/be/seat/booking/service/BookingOptionsService.java` | 의존성 교체 |
| 5 | `seat/src/main/resources/application.yaml` | cache 설정 추가 |

---

## 7. 검증 계획

### 7-1. 빌드

```bash
./gradlew :seat:build -x test
```

### 7-2. 로컬 API 테스트

```bash
# 1. 유효한 matchId로 옵션 저장 → 200
# 2. 존재하지 않는 matchId → MATCH_NOT_FOUND
# 3. 동일 matchId 재호출 → DB 쿼리 미발생 확인 (show-sql=true 또는 /actuator/caches)
```

### 7-3. 부하테스트 재실행

- 1000 VU, 동일 시나리오
- 기대: Seat P95/P99가 Queue와 유사한 수준(~2000~3000ms)으로 수렴

### 7-4. 캐시 통계 확인

`/actuator/metrics/cache.gets` 에서 `result=hit` / `result=miss` 비율 확인 (recordStats 활성화됨)

---

## 8. 롤백 계획

문제 발생 시 `application.yaml`에서 cache 설정만 제거하거나, `MatchExistenceValidator.validateExists`에서 `@Cacheable` 어노테이션만 제거해도 DB 직접 조회로 폴백된다. (기능은 유지)
