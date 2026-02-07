# goormgb-backend
구름공방 백엔드 레포지토리

## 목차
- [커밋 메시지 규칙](#커밋-메시지-규칙)
- [PR 작성 규칙](#pr-작성-규칙)
- [PR 승인 규칙](#pr-승인-규칙)
- [브랜치 전략](#브랜치-전략)
- [브랜치 보호 규칙](#브랜치-보호-규칙)

---

## 커밋 메시지 규칙

### 7가지 규칙

1. **타입은 영어 소문자로 작성**
2. 제목과 본문은 **빈 줄(엔터)**로 구분
3. 제목은 **50자 이내 한글로 작성**
4. 제목 끝에 마침표(`.`)를 찍지 않는다
5. 제목은 **명령문 형태**, **과거형 금지**
6. 본문 각 행은 **72자 이내**
7. **무엇을 작업 했는지** 설명 (어떻게는 PR에 기술)

### 타입 분류

| 타입 | 설명 |
| --- | --- |
| **feat** | 새로운 기능 추가 |
| **fix** | 버그 수정 |
| **build** | 빌드 관련 변경 (모듈 설치/삭제 등) |
| **chore** | 자잘한 변경 (코드 영향 없음) |
| **ci** | CI/CD 관련 설정 변경 |
| **docs** | 문서 수정 |
| **style** | 포맷팅, 세미콜론 등 비기능적 수정 |
| **refactor** | 코드 리팩터링 |
| **test** | 테스트 코드 추가/수정 |
| **perf** | 성능 개선 |

### 커밋 메시지 구조

```
타입(스코프): 제목

본문

바닥글
```

- **Header(필수)**, Body/Footer(선택)
- 스코프 예: auth, order, product, build, deps (선택)
- Footer: 참조 정보 (예: `resolves: #1137`), 이슈번호 (예: `fixes: #42`)

### 커밋 메시지 예시

```
feat(order): 주문 생성 API 구현

- 주문 요청 DTO 생성
- 주문 생성 시 재고 차감 로직 추가

fixes: #121
```

---

## PR 작성 규칙

> **핵심 철학: "이 PR에서 내가 한 일 목록 + 그 의도"**

### PR 제목 형식

```
[type](scope): subject
```

### PR 본문 템플릿

```markdown
## 🔧 작업 내용
- 무엇을 개발했는지
- 어떤 문제를 해결했는지
- 왜 이런 방식으로 구현했는지

## 🧩 구현 상세 (선택)
- 핵심 로직 설명
- 설계 / 구조 / 알고리즘 / 모델 선택 이유
- 트레이드오프 또는 고민했던 지점

### 📌 관련 Jira Issue
- GRGB-XX

## 🧪 테스트 방법 (선택)
- 테스트 대상
- Endpoint / 함수 / 스크립트
- 파라미터 및 체크 포인트

## ❗ 참고 사항
- 리뷰 시 유의할 점
- 후속 작업 예정
- 배포 시 주의 사항
```

### PR 작성 예시 (백엔드)

```markdown
제목: feat(order): 주문 생성 API 구현

## 🔧 작업 내용
- 주문 생성 API를 신규 구현했습니다.
- 장바구니/바로구매 주문 흐름을 하나의 API로 통합했습니다.
- 주문 시점의 배송 정보를 스냅샷으로 저장해 이후 변경에 영향을 받지 않도록 설계했습니다.

## 🧩 구현 상세 (선택)
- 주문 요청 시 addressId를 기준으로 배송지 정보를 조회하여 Receiver로 복사 저장했습니다.
- 재고 차감은 동시성 이슈를 방지하기 위해 서비스 레벨에서 처리했습니다.
- 주문 타입에 따라 장바구니 정리 로직을 분기 처리했습니다.

### 📌 관련 Jira Issue
- GRGB-46

## 🧪 테스트 방법
- 주문 생성 API 테스트
- Endpoint: POST /api/orders
- 정상 주문 / 재고 부족 / 포인트 초과 사용 케이스 확인

## ❗ 참고 사항
- 추후 결제 도메인과의 이벤트 연계가 예정되어 있습니다.
```

---

## PR 승인 규칙

> 코드 리뷰는 단순히 잘못을 찾는 과정이 아니라, **팀의 지식을 공유**하고 **코드 품질을 상향 평준화하는 과정**입니다.

### 기본 원칙

- **승인 조건**: 최소 **1명 이상의 Approve**가 있어야 merge 가능
- **예외**: 운영 장애 대응을 위한 긴급 Hotfix
- 리뷰어는 **건설적이고 맥락이 있는 피드백** 제공
- 작업자는 피드백을 **개선의 기회로 받아들이는 태도** 유지

### 백엔드 PR 리뷰 담당

- **주요(Maintainer) 작업자**: `강슬기`, `유의진`
- **Core Contributor**: `황시연` (풀스택)
- **리뷰 참여자**: `강슬기`, `유의진`, `황시연`

### 리뷰 포인트

- API 설계 일관성
- 트랜잭션/예외 처리
- 도메인 책임 분리
- 성능 및 확장성 고려 여부

> 백엔드 PR은 **최소 1명 이상(가능하면 도메인 외 1명 포함)** 리뷰 후 승인

---

## 브랜치 전략

### 주요 브랜치 (Protected)

```
feat/fix/docs (작업 브랜치)
        ↓ PR Squash & Merge
       dev (개발/Dev)
        ↓ PR Squash & Merge
       main (운영/Prod)
        ↑
    hotfixes/* (긴급 수정) ← main에서 분기
```

| 브랜치 | 설명 |
| --- | --- |
| **main** | 운영 배포 브랜치. 직접 커밋 금지. PR로만 반영 |
| **dev** | 기능 통합 브랜치. feature들이 모이는 곳 (개발/알파 환경 테스트) |
| **feat/*** | 기능 개발 브랜치 (예: feat/order-create) |
| **hotfixes/*** | 운영(main)에서 터진 긴급 수정 브랜치 |

### 백엔드 Merge Flow (작업 절차)

1. **기능 개발**: dev → feat/* 생성
2. **기능 완료**: feat/* → dev PR (Squash)
3. **배포**: dev → main PR (Squash)
4. **운영 긴급 수정**: main → hotfixes/* 생성
5. **핫픽스 반영**: hotfixes/* → main 머지 후 **반드시 dev에도 동일 변경 반영**

> ⚠️ (5)이 빠지면 다음 배포 때 **핫픽스가 덮여서 버그 재발**

### 작업 브랜치 네이밍 규칙

- **형식**: `타입/작업-요약`
- **규칙**:
  - 영문 소문자
  - 공백 대신 하이픈(-)
  - "무엇을" 중심으로 짧게

**예시**:
- `feat/order-create-api`
- `fix/payment-amount-calc`
- `refactor/auth-service-layer`
- `docs/api-spec-order`

### Merge 방식

| 상황 | Merge 방식 |
| --- | --- |
| 일반 PR | **Squash & Merge** |
| Hotfix PR | **Merge Commit** (운영 장애 대응 이력 확보) |

**Squash 사용 이유**:
- 브랜치 간 히스토리가 "PR 단위"로만 남아 추적이 쉬움
- 잔 커밋이 dev/main 로그를 오염시키지 않음
- 릴리즈 단위 회귀/롤백 시 "어떤 PR이 들어갔는지" 바로 확인 가능

### Hotfix 정책

1. **브랜치 생성**: main에서 `hotfixes/*` 분기
2. **반영 순서**:
   - hotfixes/* → main (Merge Commit)
   - hotfixes/* → dev (Backport)

### Merge 충돌 해결 원칙

- **로컬에서 해결** 후 push → PR 업데이트
- GitHub 웹 에디터로 충돌 해결 금지
- 충돌이 잦다면: 로컬에서 `merge dev`로 해결 후 정리

---

## 브랜치 보호 규칙

| 항목 | 설정 | 설명 |
| --- | --- | --- |
| Restrict deletions | ✅ | main 브랜치 삭제 금지 |
| Require linear history | ✅ | Merge 시 커밋 히스토리 일관성 유지 |
| Require pull request before merging | ✅ | PR을 통해서만 병합 가능 |
| Required approvals | ✅ 1명 | 최소 1명의 리뷰어 승인 필요 |
| Require conversation resolution | ✅ | 리뷰 코멘트 모두 해결 후 머지 가능 |
| Block force pushes | ✅ | 강제 푸시 금지 |
| Allowed merge methods | Squash only | Squash만 허용 (Hotfix 제외) |
| Allow auto-merge | ✅ | 조건 충족 시 자동 merge |
| Always suggest updating PR branches | ✅ | 베이스 브랜치 변경 시 업데이트 제안 |

### 요약

| 항목 | 규칙 |
| --- | --- |
| Merge 방법 | PR + 1명 승인 |
| 금지 | main 직접 푸시 / force push |
| Merge 방식 | Squash only (Hotfix는 Merge Commit) |
| 자동 병합 | 보호 규칙 성립 + PR 승인 후 자동 Squash 병합 |