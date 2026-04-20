# 02. 테스트 시나리오 — Flow 설명

> 각 `flow.js` 스크립트가 호출하는 엔드포인트 조합과 순서를 정리.

---

## 0. Flow 공통 구조

k6 스크립트에서 **Flow**는 한 VU가 실행할 **요청 시퀀스**다. 실제 유저가 밟는 단계(경기 상세 → 대기열 → 좌석 → 결제)를 시뮬레이션하기 위해 여러 엔드포인트를 순차적으로 호출한다.

```
VU 1개 = 1명의 가상 사용자
  └─ iteration 반복 (duration 만료까지)
       ├─ Phase 1: booking-options
       ├─ Phase 2: queue-enter
       ├─ Phase 3: queue-status 폴링 (READY까지)
       ├─ Phase 4: rec-seat-entry
       ├─ Phase 5: rec-blocks
       ├─ Phase 6: rec-assign
       ├─ Phase 7: order-sheet
       ├─ Phase 8: order-create
       └─ Phase 9: order-payment
```

Flow가 실패하면 중간에서 끊기므로, 각 Phase의 성공률/응답시간이 전체 흐름의 건강도를 나타낸다.

---

## 1. Queue Flow (`queue/flow.js`)

**목적**: 예매 옵션 저장 + 대기열 진입 기본 동작 검증 (경량 부하 측정용)

**호출 엔드포인트**:

| # | Service | Endpoint | Method |
|---|---------|----------|--------|
| 1 | Seat | `/seat/matches/{matchId}/booking-options` | POST |
| 2 | Queue | `/queue/matches/{matchId}/enter` | POST |

**특징**:
- VU당 iteration마다 한 번씩만 실행
- 폴링/좌석 선택은 생략 → 가장 가벼운 Flow
- Phase 1 ~ Phase 4에서 반복적으로 이 Flow로 병목을 측정

---

## 2. Seat Flow (`seat/flow.js`) — 추천 OFF (포도알)

**목적**: 추천 OFF 상태에서 **포도알(블럭 좌석맵) 직접 선택 → Hold** 플로우 전체 검증

**호출 엔드포인트**:

| # | Service | Endpoint | Method | 설명 |
|---|---------|----------|--------|------|
| 1 | Seat | `/seat/matches/{matchId}/booking-options` | POST | recommendationEnabled=false |
| 2 | Queue | `/queue/matches/{matchId}/enter` | POST | 대기열 진입 |
| 3 | Queue | `/queue/matches/{matchId}/status` | GET | READY까지 폴링 (1.5~5초 간격) |
| 4 | Seat | `/seat/matches/{matchId}/seat-groups` | GET | 포도알(블럭) 목록 조회 |
| 5 | Seat | `/seat/matches/{matchId}/sections/{sectionId}/blocks` | GET | 섹션별 블럭 상세 (AVAILABLE 좌석) |
| 6 | Seat | `/seat/matches/{matchId}/seat-holds` | POST | 좌석 Hold 요청 (1~8석) |

**특징**:
- `admissionToken`을 Set-Cookie에서 추출하여 후속 요청에 전달 (HttpOnly 쿠키)
- 좌석 Hold 시 **409 (이선좌)**가 발생하면 **다른 블럭으로 재시도** (최대 5회)
- 이선좌 경합은 정상 동시성 제어 결과이지만, 반복되면 좌석 부족 상태
- 동적 폴링 간격: 서버가 `pollingMs` 내려주는 값 사용 (rank ≤100 → 1.5s, ≤1000 → 3s, >1000 → 5s)

---

## 3. Recommendation Flow (`recommendation/flow.js`) — 추천 ON

**목적**: 추천 ON 상태에서 **블럭 추천 → 자동 배정** 전체 플로우 검증

**호출 엔드포인트**:

| # | Service | Endpoint | Method | 설명 |
|---|---------|----------|--------|------|
| 1 | Seat | `/seat/matches/{matchId}/booking-options` | POST | recommendationEnabled=true, ticketCount |
| 2 | Queue | `/queue/matches/{matchId}/enter` | POST | 대기열 진입 |
| 3 | Queue | `/queue/matches/{matchId}/status` | GET | READY까지 폴링 |
| 4 | Seat | `/seat/matches/{matchId}/recommendations/seat-entry` | GET | 추천 좌석 진입 (세션 초기화) |
| 5 | Seat | `/seat/matches/{matchId}/recommendations/blocks` | GET | 추천 블럭 리스트 (1~10순위) |
| 6 | Seat | `/seat/matches/{matchId}/recommendations/blocks/{blockId}/assign` | POST | 블럭 선택 → 자동 N연석 배정 |

**특징**:
- 블럭 배정 시 **1순위부터 순차 시도**
  - `200/201` → 성공, 종료
  - `409` (경합) → 같은 순위 1회 재시도 후 다음 순위
  - `404` (연석 없음) → 바로 다음 순위
  - `410` (admissionToken 만료) → 중단
- **실연석/준연석 구분**: 응답의 `semiConsecutive` 필드로 측정
- 모든 블럭 시도 실패 시 좌석 배정 실패

---

## 4. Order Flow (`order/flow.js`) — E2E 전체

**목적**: 추천 배정 성공 후 **주문서 → 주문 생성 → 결제**까지 엔드투엔드 검증

**호출 엔드포인트 (9단계)**:

| # | Service | Endpoint | Method | 설명 |
|---|---------|----------|--------|------|
| 1 | Seat | `/seat/matches/{matchId}/booking-options` | POST | recommendationEnabled=true |
| 2 | Queue | `/queue/matches/{matchId}/enter` | POST | 대기열 진입 |
| 3 | Queue | `/queue/matches/{matchId}/status` | GET | READY까지 폴링 |
| 4 | Seat | `/seat/matches/{matchId}/recommendations/seat-entry` | GET | 추천 세션 진입 |
| 5 | Seat | `/seat/matches/{matchId}/recommendations/blocks` | GET | 추천 블럭 리스트 |
| 6 | Seat | `/seat/matches/{matchId}/recommendations/blocks/{blockId}/assign` | POST | 좌석 자동 배정 (Hold 5분) |
| 7 | Order-Core | `/order/mypage/orders/sheet?matchId={m}&seatIds={s}` | GET | 주문서 조회 (가격 계산) |
| 8 | Order-Core | `/order/mypage/orders` | POST | 주문 생성 (PAYMENT_PENDING) |
| 9 | Order-Core | `/order/mypage/orders/{orderId}/payment` | POST | 결제 처리 (TOSS_PAY/KAKAO_PAY/BANK_TRANSFER) |

**특징**:
- 추천 Flow의 모든 단계 + 주문/결제 3단계 추가
- 주문서 조회에서 **서버사이드 가격 재계산** (클라이언트 조작 방지)
  - `totalPrice = Σ(seatPrice) + 2000` (예매 수수료)
- 결제 완료 후 Kafka `payment-completed` 이벤트 → Seat가 `BLOCKED → SOLD` 전환
- 경기당 최대 **8매 제한** (`MAX_TICKETS_PER_ORDER`)

---

## 5. Flow 간 복합 엔드포인트 호출 요약

```
┌─────────────────────────────────────────────────────────────────────┐
│                     Flow별 호출 엔드포인트 매트릭스                    │
├──────────────────────────────────────┬──────┬─────┬─────┬───────────┤
│ Endpoint                             │Queue │Seat │Rec  │Order(E2E) │
├──────────────────────────────────────┼──────┼─────┼─────┼───────────┤
│ POST  /seat/.../booking-options      │  ✓   │  ✓  │  ✓  │    ✓      │
│ POST  /queue/.../enter               │  ✓   │  ✓  │  ✓  │    ✓      │
│ GET   /queue/.../status              │      │  ✓  │  ✓  │    ✓      │
│ GET   /seat/.../seat-groups          │      │  ✓  │     │           │
│ GET   /seat/.../sections/{s}/blocks  │      │  ✓  │     │           │
│ POST  /seat/.../seat-holds           │      │  ✓  │     │           │
│ GET   /seat/.../rec/seat-entry       │      │     │  ✓  │    ✓      │
│ GET   /seat/.../rec/blocks           │      │     │  ✓  │    ✓      │
│ POST  /seat/.../rec/blocks/{b}/assign│      │     │  ✓  │    ✓      │
│ GET   /order/.../orders/sheet        │      │     │     │    ✓      │
│ POST  /order/.../orders              │      │     │     │    ✓      │
│ POST  /order/.../orders/{o}/payment  │      │     │     │    ✓      │
└──────────────────────────────────────┴──────┴─────┴─────┴───────────┘
```

---

## 6. Flow별 부하 특성

| Flow | RPS 경향 | 주요 병목 지점 | 측정 목적 |
|------|---------|--------------|----------|
| **Queue** | 높음 (300+ RPS) | DB 커넥션, booking-options 저장 | **최소 부하로 DB/커넥션 풀 측정** |
| **Seat (추천 OFF)** | 중간 (280 RPS) | 포도알 조회 (N+1 쿼리), 좌석 경합 | 동시 좌석 선택 경합 검증 |
| **Recommendation (추천 ON)** | 중간 (290 RPS) | 추천 블럭 계산, 블럭 분산 락 | **핵심 기능** 안정성 검증 |
| **Order E2E** | 낮음 (190 RPS) | 전체 체인, 주문 생성 Hold 검증 | **엔드투엔드 안정성** 검증 |

---

## 7. Flow 데이터 준비

### 7.1 MATCH_IDS

Flow는 `MATCH_IDS` 환경변수로 대상 경기를 설정한다.
- 단일: `MATCH_IDS=100`
- 복수 로테이션: `MATCH_IDS=100,101,102` → VU/iteration 별로 rotation

### 7.2 로그인 계정

k6는 **1,001개의 부하테스트 전용 계정** (`loadtest-login`)을 순환 사용한다.
- 각 VU당 별도 계정 → 동시 인증 흐름 시뮬레이션
- Refresh Token TTL은 15분 (부하테스트용 단축)
- 기본 RefreshToken은 4시간 TTL (Redis 메모리 누적 방지)

### 7.3 AuthMode

| 모드 | 설명 |
|------|------|
| `multiple` | 여러 계정을 VU별로 순환 (기본) |
| `single` | 모든 VU가 같은 계정 사용 (인증 캐시 테스트용) |

---

## 8. 폴링 간격 동적 조절

Queue 서비스는 클라이언트에 `pollingMs`를 내려보내, 클라이언트가 서버 부하에 맞춰 폴링 간격을 조절하도록 한다.

| 대기 순위 | `pollingMs` | 효과 |
|----------|------------|------|
| rank ≤ 100 | 1,500ms | 곧 입장 가능한 유저에 빠른 피드백 |
| rank ≤ 1,000 | 3,000ms | 중간 대기 |
| rank > 1,000 | 5,000ms | 긴 대기 유저는 서버 부하 절감 |
| `READY` 상태 | 1,000ms | 입장 가능 알림 빈도 |

k6도 이 값을 존중하여 실제 사용자 폴링을 재현한다.

---

## 9. 예외 상황 처리

### 9.1 429 (Too Many Requests) — CDN/ALB Rate Limit

- 발생 시 백오프: `pollIntervalSec * 1.5`, 최대 10초
- 최대 3회 재시도 (booking-options, queue-enter)

### 9.2 409 (Conflict) — 좌석 경합

- **이선좌 = 다른 유저가 이미 선점** (정상 동시성 제어 결과)
- Seat Flow: 다른 블럭(포도알)으로 재시도 (최대 5회)
- Recommendation Flow: 같은 순위 1회 재시도 → 다음 순위

### 9.3 410 (Gone) — Admission Token 만료

- 좌석 진입 15분 초과
- 재시도 없이 중단 (대기열 재진입 필요)

### 9.4 404 (Not Found)

- `/recommendations/blocks`: 선호 블럭 내 연석 없음 → Flow 종료
- `/recommendations/blocks/{b}/assign`: 블럭 내 연석 없음 → 다음 순위

---

이 Flow 구조를 이해하면, 각 테스트 결과의 **병목 지점**을 정확히 짚을 수 있다.
예를 들어 `booking-options`이 1.5s, `rec-blocks`가 1.2s라면 Seat 서비스 내부에서 DB 조회/캐시 miss가 발생하고 있다는 의미다.
