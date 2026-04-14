# Phase 1: Seat/Queue/Auth Caffeine 캐시 확대 작업계획서

- **지라 티켓**: GRGB-XXX (추천 이름: `부하테스트 DB 커넥션 풀 소진 대응 - Caffeine 캐시 전면 확대 (Phase 1)`)
- **브랜치**: `perf/multi-service-caffeine-cache-expansion`
- **작성일**: 2026-04-15
- **관련 트러블슈팅**: `docs/트러블슈팅/부하테스트-DB커넥션풀-한계-503-트러블슈팅.md`

---

## 1. 배경

staging 부하테스트(3000 VU)에서 503 에러가 다수 발생했으며, 원인은 **DB 인스턴스(`db.t4g.small`)의 `max_connections` 270 한계**에 도달한 것으로 진단되었다.

현재 코드 레벨에서 가능한 가장 효과적인 조치는 **DB를 때리는 쿼리 자체를 앱단 캐시로 흡수하는 것**이다. 기존에 Seat 서비스의 `match-exists` 한 건만 적용되어 있던 Caffeine 캐시를, **Seat/Queue/Auth-Guard/Order-Core 전반에 걸친 정적/준정적 데이터 조회 지점으로 확대**한다.

> DB 인스턴스 업그레이드는 본 계획의 범위 밖이며, 별도 인프라 작업으로 다룬다.

---

## 2. 현재 상태 (AS-IS)

| 서비스         | 현재 적용된 캐시                    | 주요 DB 조회 지점 (캐시 미적용)                           |
|-------------|-----------------------------|----------------------------------------------|
| Seat        | `match-exists` (exists 검증) | `findDetailByIdOrThrow`, `Section`, `Block` 조회 |
| Queue       | 없음                          | `MatchRepository.findByIdOrThrow`            |
| Auth-Guard  | 없음                          | `UserRepository.findByIdOrThrow`             |
| Order-Core  | 없음                          | `findDetailByIdOrThrow`, `UserRepository`     |

### 부하테스트 결과 (3000 VU)

| 지표    | Queue    | Seat     |
|-------|----------|----------|
| P95   | 6544ms   | 6177ms   |
| 503   | 40건 (1.7%) | 18건 (0.3%) |

---

## 3. 변경 상태 (TO-BE)

### 3-1. 캐시 대상 확정

| 캐시명                  | 대상 엔티티 / 조회              | 적용 서비스                        | TTL    | 크기   | 비고                              |
|----------------------|-------------------------|-------------------------------|--------|------|---------------------------------|
| `match-exists`       | `existsById(matchId)`   | Seat (기존)                     | 10m    | 1000 | 기존 유지                           |
| `match-detail`       | `findDetailByIdOrThrow` (Match + home/away/stadium JOIN FETCH) | Seat, Order-Core, Queue | 10m | 1000 | 가장 중요 |
| `section-all`        | `findAllWithAreaOrderBy...` (섹션 전체 + Area) | Seat                  | 1h     | 100  | 스타디움 구조 - 거의 불변   |
| `blocks-by-section-ids` | `findBySectionIdInOrderBy...` | Seat                     | 1h     | 500  | 스타디움 구조 - 거의 불변   |

### 3-2. 기대 효과 (추정)

| 지표                   | 현재             | 예상 (1500 VU 기준) |
|----------------------|----------------|----------------|
| Seat DB 쿼리 (요청당)     | 3~5회           | 0~1회 (cold 시만) |
| Queue DB 쿼리 (요청당)    | 1~2회           | 0회 (cold 시 제외)  |
| Order-Core DB 쿼리 (요청당) | 2~3회           | 0~1회 (cold 시만) |
| **DB 커넥션 활성수 (peak)**  | **250 근접**     | **120~150**    |
| **P95 (1500 VU)**    | -              | **<1s 목표**     |
| **503 발생**           | 1.7% (Queue)   | <0.1%          |

---

## 4. 상세 설계

### 4-1. 공통 캐시 설정 위치

두 가지 전략 중 **옵션 B**를 채택한다.

**(A) 공통 CacheConfig를 common-core에 두기**

- 장점: 설정 중복 제거
- 단점: 서비스마다 `@EnableCaching` 필요 + 원치 않는 캐시가 다른 서비스에도 생성될 수 있음

**(B) 서비스별 CacheConfig 유지 + 캐시명/TTL만 통일 ✅**

- 장점: 서비스별로 필요한 캐시만 활성화, 블라스트 레이디어스 제어
- 단점: 일부 설정 중복 (수용 가능 수준)

### 4-2. Seat 서비스

#### 신규 컴포넌트

```
seat/booking/service/MatchExistenceValidator.java          (기존)
seat/common/service/MatchDetailCacheService.java          (신규) — findDetailByIdOrThrow 캐싱
seat/common/service/SectionLookupCacheService.java        (신규) — Section/Block 정적 조회 캐싱
```

#### 수정 파일

- `SeatCommonService.java`: `matchRepository.findDetailByIdOrThrow(matchId)` → `matchDetailCacheService.getDetail(matchId)` 로 교체
- `SeatCommonService.java`: `sectionRepository.findAllWithAreaOrderBy...` → `sectionLookupCacheService.findAllSections()` 로 교체
- `SeatCommonService.java`: `blockRepository.findBySectionIdIn...` → `sectionLookupCacheService.findBlocksBySectionIds(ids)` 로 교체

#### application.yaml 변경

```yaml
spring:
  cache:
    type: caffeine
    cache-names: match-exists,match-detail,section-all,blocks-by-section-ids
    caffeine:
      spec: recordStats
```

> 캐시별 TTL/size는 각 `@Cacheable` 메서드 위치에서 프로그래매틱하게 지정(아래 설계 참조) 또는 `CaffeineSpec`을 캐시별로 분리한다.

### 4-3. Queue 서비스

#### 신규 파일

- `Queue/.../config/CacheConfig.java` (Seat의 것과 동일 구조)
- `Queue/.../queue/service/MatchExistenceValidator.java` (Seat에서 복제 패턴) — `findByIdOrThrow` 캐싱용

#### 수정 파일

- `QueueService.java:56` 부근 `matchRepository.findByIdOrThrow(matchId)` → validator 호출로 교체

#### build.gradle

- `spring-boot-starter-cache`, `caffeine` 의존성 추가

### 4-4. Auth-Guard / Order-Core

**Phase 1 범위 확인 필요** — 매 요청마다 `UserRepository.findByIdOrThrow` 호출되는 지점이 많다.

- Auth-Guard `/auth/me` 같은 read-heavy 엔드포인트는 Redis 분산 캐시가 더 적합 (Phase 2로 분리)
- Phase 1에서는 Order-Core의 **Match 조회만** `match-detail` 캐시로 통일

### 4-5. 캐시별 TTL 정밀 설정 방식

Spring Boot는 기본적으로 **하나의 `caffeine.spec`을 모든 캐시에 동일 적용**한다. 캐시별로 다른 TTL/size를 주려면:

```java
@Configuration
@EnableCaching
public class CacheConfig {

    @Bean
    public CacheManager cacheManager() {
        CaffeineCacheManager manager = new CaffeineCacheManager();
        manager.registerCustomCache("match-exists",
            Caffeine.newBuilder().maximumSize(1000).expireAfterWrite(Duration.ofMinutes(10)).recordStats().build());
        manager.registerCustomCache("match-detail",
            Caffeine.newBuilder().maximumSize(1000).expireAfterWrite(Duration.ofMinutes(10)).recordStats().build());
        manager.registerCustomCache("section-all",
            Caffeine.newBuilder().maximumSize(100).expireAfterWrite(Duration.ofHours(1)).recordStats().build());
        manager.registerCustomCache("blocks-by-section-ids",
            Caffeine.newBuilder().maximumSize(500).expireAfterWrite(Duration.ofHours(1)).recordStats().build());
        return manager;
    }
}
```

이 방식이 가독성/유지보수성 모두 우수하므로 본 작업에서 채택한다.

### 4-6. 캐시 키 설계

- `match-exists`: `#matchId`
- `match-detail`: `#matchId`
- `section-all`: 파라미터 없음 → SpEL `"'all'"` 고정 키 사용
- `blocks-by-section-ids`: `#sectionIds.hashCode()` 또는 정렬된 키 (주의: 리스트 key는 `hashCode` 불안정 가능 → String join 권장)

---

## 5. 작업 순서

### Step 1. Seat 확장 (Day 1)

1. `MatchDetailCacheService` + `SectionLookupCacheService` 생성
2. `SeatCommonService` 리팩터링
3. `CacheConfig` 업데이트 (4-5 방식)
4. 빌드 + 로컬 테스트

### Step 2. Queue 신규 적용 (Day 1)

1. `build.gradle` 의존성 추가
2. `CacheConfig`, `MatchExistenceValidator` 생성
3. `QueueService` 리팩터링

### Step 3. Order-Core Match 캐싱 (Day 2)

1. `build.gradle` 의존성 추가
2. `CacheConfig`, `MatchDetailCacheService` 생성
3. `OrderService` 리팩터링

### Step 4. 부하테스트 재실행 + 지표 수집 (Day 2)

- 1500 VU로 먼저 측정 → P95 <1s 목표 확인
- 각 서비스 `/actuator/metrics/cache.gets?tag=result:hit` hit rate 확인

---

## 6. 수정 파일 요약

| # | 파일                                                                      | 변경      |
|---|-------------------------------------------------------------------------|---------|
| 1 | `Seat/src/main/java/.../seat/config/CacheConfig.java`                   | 수정      |
| 2 | `Seat/src/main/java/.../seat/common/service/MatchDetailCacheService.java` | 신규      |
| 3 | `Seat/src/main/java/.../seat/common/service/SectionLookupCacheService.java` | 신규      |
| 4 | `Seat/src/main/java/.../seat/common/service/SeatCommonService.java`     | 수정      |
| 5 | `Seat/src/main/resources/application.yaml`                              | 수정      |
| 6 | `Queue/build.gradle`                                                    | 수정      |
| 7 | `Queue/src/main/java/.../queue/config/CacheConfig.java`                 | 신규      |
| 8 | `Queue/src/main/java/.../queue/queue/service/MatchExistenceValidator.java` | 신규      |
| 9 | `Queue/src/main/java/.../queue/queue/service/QueueService.java`         | 수정      |
| 10 | `Queue/src/main/resources/application.yaml`                            | 수정      |
| 11 | `Order-Core/build.gradle`                                              | 수정      |
| 12 | `Order-Core/src/main/java/.../ordercore/config/CacheConfig.java`       | 신규      |
| 13 | `Order-Core/src/main/java/.../ordercore/match/service/MatchDetailCacheService.java` | 신규 |
| 14 | `Order-Core/src/main/java/.../ordercore/order/service/OrderService.java` | 수정    |
| 15 | `Order-Core/src/main/resources/application.yaml`                       | 수정      |

---

## 7. 영향 범위 및 정합성 검토

| 대상                                        | 영향               | 정합성 전략                          |
|-------------------------------------------|------------------|--------------------------------|
| 관리자가 Match 수정 (`saleStatus` 변경 등)         | 최대 10분간 이전 값 노출  | TTL 10분 — 티켓팅 오픈 시각 전후에만 유의     |
| Match 신규 생성 후 유저가 즉시 진입                   | `unless="!#result"` 로 negative caching 방지됨 | 기존 설계 유지 |
| Stadium Section/Block 변경 (구조 변경)           | 최대 1시간 이전 구조 노출  | 운영상 구조 변경은 배포 동반 → 재시작으로 캐시 초기화 |
| 동시 요청 캐시 stampede                        | Caffeine은 동일 키 요청을 1회로 수렴시킴  | 별도 조치 불필요 |
| 멀티 Pod 간 캐시 불일치                          | 인스턴스별로 TTL 만료 시점 상이 | Match/Section은 정적에 가까워 수용 가능 |

---

## 8. 검증 계획

### 8-1. 빌드

```bash
./gradlew :Seat:build :Queue:build :Order-Core:build -x test
```

### 8-2. 로컬 API 테스트

1. 유효한 matchId로 각 API 2회 호출 → 2번째 호출 시 DB 쿼리 발생 안 함 확인 (`show-sql` 로그)
2. 존재하지 않는 matchId → `MATCH_NOT_FOUND`
3. `/actuator/caches` 로 캐시 내용 확인

### 8-3. 캐시 통계 확인

```bash
curl localhost:8082/seat/actuator/metrics/cache.gets?tag=cache:match-detail
curl localhost:8082/seat/actuator/metrics/cache.gets?tag=cache:section-all
```

기대: **hit rate 95% 이상**

### 8-4. 부하테스트 (순차 상향)

| 단계 | VU    | 목표 P95  | 목표 성공률  |
|----|-------|---------|---------|
| 1  | 1000  | <500ms  | >99.9%  |
| 2  | 1500  | <1s     | >99.9%  |
| 3  | 2000  | <2s     | >99%    |
| 4  | 3000  | <3s     | >98%    |

> 5000 VU는 **DB 업그레이드 후** 재시도 (본 계획 범위 밖).

---

## 9. 롤백 계획

각 서비스의 `application.yaml`에서 `spring.cache.type: none` 으로 변경하면 `@Cacheable`이 모두 no-op 처리되어 DB 직접 조회로 즉시 폴백된다. 코드 롤백 없이도 안전하게 비활성화 가능하다.

---

## 10. Phase 2 후보 (본 계획 범위 밖)

- Auth-Guard `/auth/me` Redis 분산 캐시 (TTL 30s)
- `UserRepository` Redis 캐싱 (Order-Core 마이페이지, Auth 전반)
- `OnboardingPreference` Redis 캐싱 (추천 서비스)
- Seat `seat-groups` 응답 자체 Redis 캐시 (5~10s)

---

## 11. 산출물

- 코드 변경 (위 6절 15개 파일)
- 부하테스트 전/후 비교 보고서 → `docs/staging/load-test-phase1-result.md`
- 각 서비스 `/actuator/metrics/cache.gets` 스크린샷