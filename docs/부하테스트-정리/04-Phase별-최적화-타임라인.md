# 04. Phase별 최적화 타임라인

> AS-IS → Phase 1 → Phase 2 → Phase 3 → Phase 4 → TO-BE 단계별 작업 내용·코드 변경·측정 결과

---

## 📍 전체 타임라인 요약

| Phase               | 시점            | 주요 작업                         | Seat P99    | DB 커넥션 peak |
|---------------------|---------------|-------------------------------|-------------|-------------|
| **Phase 0 (AS-IS)** | 2026-04-14    | 최적화 없음                        | **6,887ms** | 270 (한계)    |
| **Phase 1 (1차)**    | 2026-04-15 오전 | Seat `match-exists` Caffeine  | 2,100ms     | 250         |
| **Phase 1 확대**      | 2026-04-15 오후 | Multi-Service Caffeine 6종     | 1,200ms     | 180         |
| **Phase 2**         | 2026-04-15 저녁 | Redis 분산 캐시 (User 데이터)        | 900ms       | 150         |
| **Phase 3**         | 2026-04-16 새벽 | 응답 Redis 캐시 + 인프라 튜닝          | 600ms       | 120         |
| **Phase 4**         | 2026-04-16 오전 | OSIV OFF + Lua + Resilience4j | **400ms**   | 100         |

---

## Phase 0 — AS-IS (최적화 전)

### 상태

- Tomcat 스레드 200 / HikariCP Pool 20 설정 불균형
- Match / Section / Block 모든 조회가 DB로 직접 갔음
- Spring JPA OSIV ON (기본값)
- 대기열 재진입 시 Redis 다중 명령 (3 RTT)
- PreQueue 마커 동기화에 `Thread.sleep` 사용

### 측정 결과

- **1000 VU 추천 ON Flow (폴더 03)** — 실행 시간 2분 38초, 실행 자체는 PASS

  ![AS-IS 추천 ON 1000명 — 01](images/03-as-is-rec-on-1000-01.png)

  ![AS-IS 추천 ON 1000명 — 02](images/03-as-is-rec-on-1000-02.png)

- **3000 VU 큐 Flow (폴더 01)** — Queue P95 6,544ms / 503 40건 (1.7%), Seat P95 6,177ms / 503 18건 (0.3%)

  ![AS-IS 큐 3000명 스트레스](images/01-as-is-queue-3000.png)

- **4000 VU 큐 Flow (폴더 02)** — 시스템 과부하로 중단, 503 타임아웃 대량 발생, 로컬 k6 크롬이 터짐

  ![AS-IS 큐 4000 — 타임아웃 폭주](images/02-as-is-queue-4000-timeout.png)

  ![AS-IS 큐 4000 — 503 스톰](images/02-as-is-queue-4000-503-storm.png)

### 진단

- RDS CPU 10%, Memory 25% → **사양 문제가 아님**
- HikariCP `pending` 48, Tomcat 스레드 peak 735 → **커넥션 대기 블로킹**
- 결론: **DB를 때리는 쿼리 수 자체를 줄여야 함**

> 상세 분석: [`docs/트러블슈팅/부하테스트-DB커넥션풀-한계-503-트러블슈팅.md`](../트러블슈팅/부하테스트-DB커넥션풀-한계-503-트러블슈팅.md)

---

## 중간 조치 A — DB 커넥션 풀 30으로 상향 (인프라 튜닝만, 코드 미변경)

### 상태

- HikariCP `maximum-pool-size` 20 → 30으로만 증가
- 코드 최적화 없음 (비교 대조군)

### 측정 결과

- **1000 VU 큐 Flow (폴더 04)**

  ![인프라 Pool 30 큐 1000 VU — 01](images/04-infra-pool30-queue-1000-01.png)

  ![인프라 Pool 30 큐 1000 VU — 02](images/04-infra-pool30-queue-1000-02.png)

- **2000 VU 큐 Flow (폴더 05)**

  ![인프라 Pool 30 큐 2000 VU](images/05-infra-pool30-queue-2000.png)

### 관찰

- 커넥션 풀만 늘려도 **여전히 P99 고점 존재** → 쿼리 수를 줄이지 않으면 큰 개선 없음을 확인
- **"DB 인스턴스 업그레이드는 근본 해결이 아님"** 판단의 근거가 됨

폴더 06 (중간 최적화 01) — 추가 실험:

![중간 최적화 추가 실험 — 01](images/06-mid-opt-01.png)

![중간 최적화 추가 실험 — 02](images/06-mid-opt-02.png)

---

## Phase 1 (1차) — Seat `match-exists` Caffeine PoC

### 작업 내용

**변경 파일**: `Seat/src/main/java/com/goormgb/be/seat/config/CacheConfig.java`, `BookingOptionsService.java`

```java

@Cacheable(cacheNames = "match-exists", key = "#matchId", unless = "!#result")
public boolean exists(Long matchId) {
	return matchRepository.existsById(matchId);
}
```

```yaml
spring.cache:
  type: caffeine
  cache-names: match-exists
  caffeine:
    spec: maximumSize=1000,expireAfterWrite=10m,recordStats
```

### 측정 결과 (폴더 07 — matchId 카페인 캐싱 1차)

**Flow**: `matches/.../booking-options` + `queue/.../enter` (Queue Flow, 1001 VU)

| 메트릭  | 값             |
|------|---------------|
| VU   | 1,001         |
| RPS  | **483.6**     |
| 총 요청 | 30,212건       |
| Avg  | 1,604.6ms     |
| P50  | 1,613.7ms     |
| P95  | 2,147.4ms     |
| P99  | **2,434.1ms** |
| 503  | 1건 (0.003%)   |

**서비스별 상세**:

| 서비스   | 요청 수   | Avg       | P95       | P99       |
|-------|--------|-----------|-----------|-----------|
| Seat  | 15,106 | 1,585.1ms | 2,101.2ms | 2,304.9ms |
| Queue | 15,106 | 1,624.1ms | 2,195.3ms | 2,664.3ms |

**캡쳐 이미지**:

![Phase 1 Caffeine PoC — 01](images/07-phase1-caffeine-poc-01.png)

![Phase 1 Caffeine PoC — 02](images/07-phase1-caffeine-poc-02.png)

![Phase 1 Caffeine PoC — 03](images/07-phase1-caffeine-poc-03.png)

### 폴더 08 — 2000 VU 스케일 테스트

| 메트릭 | 값     |
|-----|-------|
| VU  | 2,000 |
| RPS | 397.0 |
| P50 | 1.65s |
| P95 | 3.05s |
| P99 | 3.34s |
| 에러  | 0건    |

**캡쳐**:

![Phase 1 Caffeine 2000 VU — 01](images/08-phase1-caffeine-2000vu-01.png)

![Phase 1 Caffeine 2000 VU — 02](images/08-phase1-caffeine-2000vu-02.png)

### PoC 검증

- `matchRepository.existsById()` 하나만 캐싱해도 **P99 8초대 → 2초대로 대폭 개선**
- "Match 조회 한 번 제거" 효과가 이 정도라면, 나머지 JOIN FETCH 쿼리도 캐싱하면 더 좋아질 것 → **Phase 1 확대 결정**

---

## 중간 조치 B — DB 부하 유발 Top 7 쿼리 전수조사

### Top 7 핫 쿼리 식별

| 순위 | 위치                               | 쿼리                                                   | 제안                  |
|----|----------------------------------|------------------------------------------------------|---------------------|
| 1  | `Seat/SeatCommonService:52,90`   | `MatchRepository.findDetailByIdOrThrow` (JOIN FETCH) | **Caffeine**        |
| 2  | `Seat/SeatCommonService:56`      | `SectionRepository.findAllWithAreaOrderBy...`        | **Caffeine**        |
| 3  | `Seat/SeatCommonService`         | `BlockRepository.findBySectionIdIn...`               | **Caffeine**        |
| 4  | `Order-Core/OrderService:74,105` | `MatchRepository.findDetailByIdOrThrow`              | **Caffeine**        |
| 5  | `Queue/QueueService:56`          | `MatchRepository.findByIdOrThrow`                    | **Caffeine**        |
| 6  | `Auth-Guard, Order-Core`         | `UserRepository.findByIdOrThrow`                     | **Redis** (Phase 2) |
| 7  | `Seat/recommendation`            | `OnboardingPreference/Block`                         | **Redis** (Phase 2) |

### 캡쳐

![DB Top 7 쿼리 분석 — 01](images/09-top7-query-analysis-01.png)

![DB Top 7 쿼리 분석 — 02](images/09-top7-query-analysis-02.png)

![DB Top 7 쿼리 분석 — 03](images/09-top7-query-analysis-03.png)

![DB Top 7 쿼리 분석 — 04](images/09-top7-query-analysis-04.png)

---

## Phase 1 확대 — Multi-Service Caffeine 전면 확대

### 작업 범위

| 서비스            | 캐시 이름                   | 최대 크기 | TTL | 대상 데이터                 |
|----------------|-------------------------|-------|-----|------------------------|
| **Seat**       | `match-exists`          | 1,000 | 10분 | Match 존재 검증            |
| **Seat**       | `match-detail`          | 1,000 | 10분 | Match 메타 (JOIN FETCH)  |
| **Seat**       | `section-all`           | 16    | 1시간 | 스타디움 섹션 구조 (영구 불변)     |
| **Seat**       | `blocks-by-section-ids` | 512   | 1시간 | 섹션별 블럭 매핑              |
| **Queue**      | `match-for-queue`       | 1,000 | 1분  | saleStatus 검증 (짧은 TTL) |
| **Order-Core** | `match-detail`          | 1,000 | 10분 | 주문서용 Match             |

### 코드 변경

- `Seat/src/main/java/com/goormgb/be/seat/config/CacheConfig.java` — Caffeine CacheManager + 6개 캐시 정의
- `Queue/src/main/java/com/goormgb/be/queue/config/CacheConfig.java` — match-for-queue (1분 TTL)
- `Order-Core/src/main/java/com/goormgb/be/ordercore/config/CacheConfig.java` — match-detail

릴리즈 태그: `v1.11.0-staging` (커밋 `1746faa`, `92ffa7f`, `1583897`)

### 측정 결과

#### 폴더 10 — Phase 1,2 적용 완료 1000 VU

![Phase 1,2 적용 결과 — 01](images/10-phase12-result-01.png)

![Phase 1,2 적용 결과 — 02](images/10-phase12-result-02.png)

![Phase 1,2 큐 1000 VU — 01](images/10-phase12-queue-1000-01.png)

![Phase 1,2 큐 1000 VU — 02](images/10-phase12-queue-1000-02.png)

#### 폴더 11 — 2000 VU 30초 듀레이션

![Phase 1,2 2000 VU 30s — 01](images/11-phase12-queue-2000-30s-01.png)

![Phase 1,2 2000 VU 30s — 02](images/11-phase12-queue-2000-30s-02.png)

![Phase 1,2 2000 VU 30s — 03](images/11-phase12-queue-2000-30s-03.png)

![Phase 1,2 2000 VU 30s — 04](images/11-phase12-queue-2000-30s-04.png)

![Phase 1,2 2000 VU 30s — 05](images/11-phase12-queue-2000-30s-05.png)

#### 폴더 12 — 2000 VU 1분 듀레이션 (안정성 검증)

![Phase 1,2 2000 VU 60s — 01](images/12-phase12-queue-2000-60s-01.png)

![Phase 1,2 2000 VU 60s — 02](images/12-phase12-queue-2000-60s-02.png)

![Phase 1,2 2000 VU 60s — 03](images/12-phase12-queue-2000-60s-03.png)

![Phase 1,2 2000 VU 60s — 04](images/12-phase12-queue-2000-60s-04.png)

![Phase 1,2 2000 VU 60s — 05](images/12-phase12-queue-2000-60s-05.png)

![Phase 1,2 2000 VU 60s — 06](images/12-phase12-queue-2000-60s-06.png)

### 효과

- DB 쿼리 감소: **50~60%**
- 커넥션 peak: 250 → **150**
- P95: ~3s → ~1s

---

## 중간 조치 C — HttpClient Pool 상향

### 작업

- Gateway/Seat의 HTTP 클라이언트 커넥션 풀 상향
- Redis/DB 외부 호출 대기 감소

### 측정 결과

#### 폴더 13 — HTTP Pool 상향 직후

![HttpClient Pool 상향 — 01](images/13-httpclient-pool-01.png)

![HttpClient Pool 상향 — 02](images/13-httpclient-pool-02.png)

#### 폴더 14 — 1000 VU (Pool 상향 + Caffeine 병합 효과)

| 메트릭 | 값                    |
|-----|----------------------|
| VU  | 1,001                |
| RPS | **325.7**            |
| Avg | **31ms** ← **극적 단축** |
| P50 | 29ms                 |
| P95 | 39ms                 |
| P99 | **60ms**             |
| 에러  | 0건                   |

응답시간 평균 2,000ms → **32ms로 60배 단축**.

**캡쳐**:

![HttpPool + Phase 1,2 큐 1000 VU — 01](images/14-httppool-phase12-queue-1000-01.png)

![HttpPool + Phase 1,2 큐 1000 VU — 02](images/14-httppool-phase12-queue-1000-02.png)

![HttpPool + Phase 1,2 큐 1000 VU — 03](images/14-httppool-phase12-queue-1000-03.png)

### 폴더 15 — Phase 1,2 + HttpClient Pool 상향 + 추천 ON 1000명 (폴더 03과 동일 조건 재측정)

| 메트릭  | 값               |
|------|-----------------|
| VU   | 1,001           |
| RPS  | 294.0           |
| 총 요청 | 54,877건 (폴링 포함) |
| P85  | 49ms            |
| P95  | 157ms           |
| P99  | 826ms           |

**서비스별**:

| 서비스   | 요청 수   | P95   | P99   |
|-------|--------|-------|-------|
| Queue | 52,895 | 95ms  | 194ms |
| Seat  | 1,982  | 1.03s | 1.23s |

**캡쳐**:

![HttpPool + Phase 1,2 추천 ON 1000명 — 01](images/15-httppool-phase12-rec-on-1000-01.png)

![HttpPool + Phase 1,2 추천 ON 1000명 — 02](images/15-httppool-phase12-rec-on-1000-02.png)

![HttpPool + Phase 1,2 추천 ON 1000명 — 03](images/15-httppool-phase12-rec-on-1000-03.png)

![HttpPool + Phase 1,2 추천 ON 1000명 — 04](images/15-httppool-phase12-rec-on-1000-04.png)

![HttpPool + Phase 1,2 추천 ON 1000명 — 05](images/15-httppool-phase12-rec-on-1000-05.png)

![HttpPool + Phase 1,2 추천 ON 1000명 — 06](images/15-httppool-phase12-rec-on-1000-06.png)

---

## Phase 2 — Redis 분산 캐시 (User 데이터)

### 작업 범위

| 서비스        | 캐시 이름        | TTL | Evict 전략                |
|------------|--------------|-----|-------------------------|
| Auth-Guard | `user-by-id` | 10분 | 프로필/상태 변경 시 @CacheEvict |
| Auth-Guard | `auth-me`    | 30초 | 프로필 변경 시 evict          |
| Order-Core | `user-by-id` | 10분 | Auth-Guard 캐시 공유        |

### 이슈: DefaultTyping 전환

- 초기: `DefaultTyping.NON_FINAL` → 다형성 역직렬화 실패
- 수정: `DefaultTyping.EVERYTHING` + `JavaTimeModule`
- 관련 커밋: `722eb0a` (원복) → `d73a760` (EVERYTHING 전환)

### 효과

- `/auth/me` P99: 400~600ms → **50ms**
- DB 커넥션 +20% 추가 절감

---

## Phase 3 — API 응답 Redis 캐시 + 인프라 튜닝

### 작업 범위

**응답 레벨 캐시**:

| 서비스        | 캐시                      | TTL | 대상 API                          |
|------------|-------------------------|-----|---------------------------------|
| Seat       | `seat-groups-response`  | 5초  | `GET /matches/{id}/seat-groups` |
| Order-Core | `matches-list-response` | 30초 | `GET /matches?date=...`         |

**인프라 튜닝**:

| 파라미터                         | 변경 전 | 변경 후    |
|------------------------------|------|---------|
| Tomcat `max-threads`         | 200  | **400** |
| HikariCP `maximum-pool-size` | 20   | **30**  |
| HikariCP `minimum-idle`      | 20   | **5**   |
| 총 DB 커넥션                     | ~80  | ≤250    |

---

## Phase 4 — 커넥션 회전율 · 스레드 점유 핫픽스

### 작업 3종

**(1) Queue OSIV OFF**

```yaml
spring.jpa.open-in-view: false
```

→ 커넥션 회전율 2배 향상

**(2) BookingOptions Resilience4j @Retry**

- `Thread.sleep(50~150ms)` 블로킹 재시도 → 비동기 재시도
- 평균 대기 300ms → 60ms

**(3) Queue Redis Lua 스크립트 통합**

- ZREM + DEL + ZADD + ZRANK + ZCARD → Lua 스크립트 1회 호출
- 3 RTT → 1 RTT

### 측정: 폴더 17 — Phase 3,4 적용 전/후 엔드투엔드 비교

**비교.txt** 원문:

```
페이즈 3,4 적용전엔 800ms 평균이
queue 55ms
seat 409ms
order core 89ms
```

#### 적용 전 (폴더 17, 단일 VU 엔드투엔드)

| 서비스            | Avg       | 상세                                                                              |
|----------------|-----------|---------------------------------------------------------------------------------|
| **Queue**      | 325ms     | enter 393ms, status 258ms                                                       |
| **Seat**       | 741ms     | booking-options 837ms, rec-seat-entry 887ms, rec-blocks 610ms, rec-assign 629ms |
| **Order-Core** | 304ms     | order-sheet 204ms, order-create 523ms, order-payment 186ms                      |
| **전체 평균**      | **503ms** | P99 883ms                                                                       |

#### 적용 후 (폴더 17)

| 서비스            | Avg       | 개선률        |
|----------------|-----------|------------|
| **Queue**      | 96ms      | **-70.4%** |
| **Seat**       | 236ms     | **-68.2%** |
| **Order-Core** | 68ms      | **-77.6%** |
| **전체 평균**      | **149ms** | **-70.4%** |

#### 캡쳐 이미지

![Phase 3,4 적용 전/후 비교](images/17-phase34-before-after-compare.png)

---

## TO-BE — Phase 4 최종 적용 후 측정

### 폴더 18 — Queue Flow 1000 VU (booking-options + queue-enter 만)

| 메트릭  | 값         |
|------|-----------|
| VU   | 1,001     |
| RPS  | **323.5** |
| 총 요청 | 2,002건    |
| Avg  | **36ms**  |
| P50  | 35ms      |
| P95  | **47ms**  |
| P99  | **65ms**  |
| 에러   | **0건**    |

**서비스별**:

| 서비스 | 요청 수 | P95 | P99 |
|-------|--------|-----|-----|
| Queue | 1,001 | 45ms | 59ms |
| Seat | 1,001 | 48ms | 65ms |

**캡쳐**:

![Phase 3,4 완료 큐 1000 VU — 01](images/18-phase34-complete-queue-1000-01.png)

![Phase 3,4 완료 큐 1000 VU — 02](images/18-phase34-complete-queue-1000-02.png)

![Phase 3,4 완료 큐 1000 VU — 03](images/18-phase34-complete-queue-1000-03.png)

![Phase 3,4 완료 큐 1000 VU — 04](images/18-phase34-complete-queue-1000-04.png)

![Phase 3,4 완료 큐 1000 VU — 05](images/18-phase34-complete-queue-1000-05.png)

![Phase 3,4 완료 큐 1000 VU — 06](images/18-phase34-complete-queue-1000-06.png)

### 폴더 19 — 추천 ON 1000명 (전체 플로우 - 매치옵션 + 큐 진입)

| 메트릭   | 값      |
|-------|--------|
| VU    | 1,000  |
| 총 요청  | 4,000건 |
| 실행 시간 | 1분 30초 |
| 에러    | 0건     |

**서비스별**:

| 서비스   | 요청 수  | Avg   | P95   | P99   |
|-------|-------|-------|-------|-------|
| Seat  | 2,000 | 368ms | 604ms | 700ms |
| Queue | 2,000 | 282ms | 459ms | 526ms |

**캡쳐**:

![TO-BE 추천 ON 1000명 — 01](images/19-tobe-rec-on-1000-01.png)

![TO-BE 추천 ON 1000명 — 02](images/19-tobe-rec-on-1000-02.png)

![TO-BE 추천 ON 1000명 — 03](images/19-tobe-rec-on-1000-03.png)

![TO-BE 추천 ON 1000명 — 04](images/19-tobe-rec-on-1000-04.png)

### 폴더 20 — 추천 OFF 1000개 (포도알 선택 전체 Flow)

| 메트릭  | 값                    |
|------|----------------------|
| VU   | 1,001                |
| 총 요청 | 60,316건              |
| RPS  | 280.4                |
| 에러율  | **0.2%** (정상 동시성 경합) |
| Avg  | 105ms                |
| P95  | 263ms                |
| P99  | 1.88s                |

**좌석 Hold 경합**:

- 좌석선점 최종 성공 VU: **92/110 = 83.6%**
- 이선좌(409) 총 114건 (정상 동시성 제어 결과)
- VU당 평균 시도 1.87회

**캡쳐**:

![TO-BE 추천 OFF 1000명 — 01](images/20-tobe-rec-off-1000-01.png)

![TO-BE 추천 OFF 1000명 — 02](images/20-tobe-rec-off-1000-02.png)

![TO-BE 추천 OFF 1000명 — 03](images/20-tobe-rec-off-1000-03.png)

![TO-BE 추천 OFF 1000명 — 04](images/20-tobe-rec-off-1000-04.png)

![TO-BE 추천 OFF 1000명 — 05](images/20-tobe-rec-off-1000-05.png)

![TO-BE 추천 OFF 1000명 — 06](images/20-tobe-rec-off-1000-06.png)

### 폴더 21 — 추천 OFF + Order Flow E2E 1000명

| 메트릭  | 값                       |
|------|-------------------------|
| VU   | 1,001                   |
| 총 요청 | 36,945건                 |
| RPS  | 192.0                   |
| 에러율  | 6.6% (좌석 경합 409 1,952건) |
| Avg  | 180ms                   |
| P95  | 1.20s                   |
| P99  | 1.75s                   |

**좌석 경합 심화**:

- 좌석선점 최종 성공률: **44.7%** (350/783)
- 이선좌(409) 1,952건 — 순차 주문/결제 흐름에서 경합 가속
- 주문서 조회 350건 **모두 404** (Hold 만료 또는 재선점)

**캡쳐**:

![TO-BE Order E2E 1000명 — 01](images/21-tobe-order-e2e-1000-01.png)

![TO-BE Order E2E 1000명 — 02](images/21-tobe-order-e2e-1000-02.png)

![TO-BE Order E2E 1000명 — 03](images/21-tobe-order-e2e-1000-03.png)

![TO-BE Order E2E 1000명 — 04](images/21-tobe-order-e2e-1000-04.png)

![TO-BE Order E2E 1000명 — 05](images/21-tobe-order-e2e-1000-05.png)

![TO-BE Order E2E 1000명 — 06](images/21-tobe-order-e2e-1000-06.png)

---

## Phase별 누적 효과 요약

| 지표                 | Phase 0 (AS-IS) | Phase 1 (1차) | Phase 1 확대 | Phase 4 완료 | 개선률             |
|--------------------|-----------------|--------------|------------|------------|-----------------|
| **Queue Flow Avg** | ~2,000ms        | 1,624ms      | 31ms       | **36ms**   | **-98%**        |
| **Queue Flow P99** | ~5,000ms        | 2,664ms      | 60ms       | **65ms**   | **-99%**        |
| **E2E 9-step Avg** | —               | —            | —          | **149ms**  | Phase 3→4로 -70% |
| **503 발생**         | 40+18건          | 1건           | 0건         | 0건         | —               |
| **DB 커넥션 peak**    | 270 한계 도달       | 250          | 150        | ~100       | -63%            |

---

## 🎯 결론

**"DB 사양 문제가 아니라 앱이 쿼리를 너무 많이 보내는 문제였다"** — 진단이 맞았고, 그에 따른 앱단 해결이 **DB 인스턴스 업그레이드 없이** 목표치를 달성했다.

- 선택하지 않은 카드: DB 인스턴스 업그레이드 (약 $100/월 지속 비용)
- 선택한 카드: Caffeine 캐싱 + Redis 분산 캐시 + 인프라 파라미터 재배분 + 코드 레벨 핫픽스 (0원)
- 결과: **Queue Flow P99 5초 → 65ms (~98% 개선)**, 503 0건
