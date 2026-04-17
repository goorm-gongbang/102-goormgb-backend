# 릴리즈 노트 가이드

## 목차

1. [릴리즈 노트란?](#릴리즈-노트란)
2. [왜 릴리즈 노트를 작성하는가?](#왜-릴리즈-노트를-작성하는가)
3. [릴리즈 노트 구성 요소](#릴리즈-노트-구성-요소)
4. [작성 시 주의사항](#작성-시-주의사항)
5. [버전 네이밍 규칙 (Semantic Versioning)](#버전-네이밍-규칙-semantic-versioning)
6. [Git 태그 관리](#git-태그-관리)
7. [GitHub Release 생성](#github-release-생성)
8. [릴리즈 노트 템플릿](#릴리즈-노트-템플릿)
9. [실제 작성 예시](#실제-작성-예시)
10. [노션 릴리즈 노트 관리](#노션-릴리즈-노트-관리)

---

## 릴리즈 노트란?

소프트웨어가 업데이트될 때마다 **변경 사항, 기능 추가/삭제, 버그 수정** 등을 체계적으로 정리하여 이해관계자에게 전달하는 문서입니다.

가장 쉬운 비유는 **게임 패치 노트**입니다. 새로운 캐릭터가 추가됐는지, 어떤 버그가 수정됐는지, 밸런스 조정이 있었는지 — 이런 내용을 정리한 문서가 릴리즈 노트입니다.

### 누가 읽는가?

| 독자         | 관심사                        |
|------------|----------------------------|
| 기획팀        | 요청한 기능이 반영됐는지, UI/UX 변경 사항 |
| 클라우드/인프라팀  | 환경변수 변경, DB 마이그레이션, 인프라 설정 |
| QA팀        | 테스트해야 할 신규 기능, 수정된 버그      |
| 프론트엔드 개발자  | API 엔드포인트 변경, 응답 형식 변경     |
| 다른 백엔드 개발자 | 내가 작업하지 않은 모듈의 변경 사항       |

---

## 왜 릴리즈 노트를 작성하는가?

1. **변경 이력 추적**: 업데이트 정보를 한 곳에 모아 기록하여, 내가 작업하지 않은 변경 사항도 빠르게 파악할 수 있습니다.
2. **제품의 발자취 파악**: 릴리즈 노트를 보면 누구나 제품이 어떻게 발전해왔는지 한눈에 확인할 수 있습니다.
3. **서비스 품질 신뢰**: 지속적으로 개선되고 있다는 것을 보여줌으로써 이해관계자의 신뢰를 확보합니다.
4. **피드백 유도**: 사용자의 의견이 반영되고 있다는 인상을 주어 적극적인 피드백을 이끌어냅니다.
5. **배포 사고 대응**: 문제 발생 시 어떤 버전에서 어떤 변경이 있었는지 빠르게 추적할 수 있습니다.

---

## 릴리즈 노트 구성 요소

릴리즈 노트의 양식은 조직마다 다르지만, 일반적으로 아래 네 가지 요소로 구성됩니다.

### 1. 버전 (Version)

배포 버전을 명시합니다. 버전만 보고도 변경 범위를 대략 유추할 수 있어야 합니다.

```
v1.3.0-staging  ← 기능 추가가 포함된 staging 배포
v1.2.1          ← 버그 수정만 포함된 prod 배포
```

### 2. 날짜 (Date)

배포 일자를 명시합니다. 일관된 형식을 사용하는 것이 중요합니다.

```
2026-03-30 (YYYY-MM-DD 형식 권장)
```

### 3. 구분 태그 (Category)

변경 사항을 빠르게 분류할 수 있도록 태그를 사용합니다.

| 태그           | 의미     | 설명                                |
|--------------|--------|-----------------------------------|
| `Feature`    | 새로운 기능 | 기존에 없던 기능의 최초 추가                  |
| `Improved`   | 개선     | 기존 기능의 성능, UX, 로직 개선              |
| `Fixed`      | 버그 수정  | 기존 기능의 오류 수정                      |
| `Hotfix`     | 긴급 수정  | 장애 대응을 위한 긴급 배포                   |
| `Changed`    | 변경     | 기존 동작 방식의 변경 (Breaking Change 포함) |
| `Deprecated` | 중단 예정  | 향후 제거될 기능 안내                      |
| `Infra`      | 인프라    | DevOps, 모니터링, Docker, CI/CD 변경    |

### 4. 설명 (Description)

변경 내용을 간결하게 서술합니다. 길어지면 관련 문서 링크를 첨부합니다.

```
# Bad  — 너무 모호함
- 버그 수정 및 성능 개선

# Good — 구체적이고 명확함
- [Fixed] 결제 완료 시 match_seat SOLD 상태 전환이 누락되던 문제 수정 (GRGB-318)
```

---

## 작성 시 주의사항

### "버그 수정 및 성능 개선"은 금지

어떤 버그가 수정되었는지, 어떤 기능이 변경되었는지 구체적으로 작성해야 합니다. 독자가 자신이 제보한 버그가 수정됐는지, 관심 있는 기능이 추가됐는지 확인할 수 있어야 합니다.

### 간결하게, 그러나 필수 정보는 빠뜨리지 않기

- 한 항목당 1~2문장이 적당합니다.
- 자세한 설명이 필요하면 본문에 쓰지 말고 **관련 문서 링크**를 첨부합니다.
- 토스페이먼츠, 카카오 i 등 실무에서도 이 방식을 사용합니다.

### 일관된 용어 사용

여러 사람이 작성할 경우, 같은 기능을 다른 용어로 표현하지 않도록 주의합니다.

```
# Bad  — 같은 기능인데 표현이 다름
- BOT Response 기능 추가
- Bot 응답 개선
- 봇의 응답 방식 개선

# Good — 용어 통일
- 봇 응답 기능 추가
- 봇 응답 성능 개선
```

### 사용자(독자) 관점에서 작성

릴리즈 노트는 독자를 위한 문서입니다. 내부 구현 디테일보다는 **"무엇이 바뀌었는지"**에 초점을 맞춥니다.

```
# Bad  — 개발자 관점
- PaymentService에 Clock 주입 및 matchSeatIds 개수 불일치 로그 추가

# Good — 독자 관점
- 결제 시 좌석 상태가 정상적으로 SOLD로 전환되지 않던 문제 수정
```

### 시각적 구분 활용

카테고리별 그룹화, 표, 구분선 등을 활용하여 가독성을 높입니다. NHN Cloud는 `[기능 추가]` 같은 대괄호 태그를, Kakao i는 표 형식을 사용하고 있습니다.

---

## 버전 네이밍 규칙 (Semantic Versioning)

[Semantic Versioning (SemVer)](https://semver.org/lang/ko/)을 기반으로 합니다.

```
v X . Y . Z [-환경]
  │   │   │    └── staging / 생략(prod)
  │   │   └── Patch: 버그 수정
  │   └── Minor: 기능 추가 (하위 호환)
  └── Major: 대규모 변경 (하위 호환 X)
```

### 각 자릿수의 의미

| 구분            | 언제 올리는가                         | 예시                  | 실제 사례                      |
|---------------|---------------------------------|---------------------|----------------------------|
| **Major (X)** | API 호환성이 깨지는 대규모 변경, 아키텍처 전면 개편 | `v1.0.0` → `v2.0.0` | 인증 체계 전면 교체, DB 스키마 대규모 변경 |
| **Minor (Y)** | 하위 호환을 유지하면서 새로운 기능 추가          | `v1.2.0` → `v1.3.0` | 마이페이지 API 추가, 좌석 추천 기능 추가  |
| **Patch (Z)** | 기존 기능의 버그 수정, 핫픽스               | `v1.3.0` → `v1.3.1` | 결제 시 좌석 상태 전환 누락 수정        |

### 환경별 태그 형식

| 환경         | 태그 형식            | 예시               |
|------------|------------------|------------------|
| Staging    | `vX.Y.Z-staging` | `v1.3.0-staging` |
| Production | `vX.Y.Z`         | `v1.3.0`         |

> `-staging` 접미사로 staging 배포와 prod 배포를 명확히 구분합니다.

### 버전 올리기 판단 기준

```
Q: 이번 배포에 새로운 API 엔드포인트가 추가되었는가?
   → Yes: Minor (Y) +1, Patch를 0으로 리셋
   → No: 버그 수정만 있는가?
         → Yes: Patch (Z) +1
         → No: 기존 API의 동작이 바뀌어 프론트가 수정해야 하는가?
               → Yes: Major (X) +1, Minor/Patch를 0으로 리셋
```

### 버전 흐름 예시

```
dev에서 기능 개발 완료
  → v1.3.0-staging 태그 (staging 배포)
  → staging에서 QA 진행
  → 버그 발견 시 dev에서 수정 후 v1.3.1-staging (staging 재배포)
  → QA 통과 후 v1.3.1 태그 (prod 배포)
```

### 우리 프로젝트의 버전 히스토리

| 태그               | 환경      | 내용                                          |
|------------------|---------|---------------------------------------------|
| `v1.1.1`         | staging | staging 배포 (2026-03-25)                     |
| `v1.2.0`         | prod    | prod 배포: 쿠키 SameSite=Lax 보안 설정 (2026-03-27) |
| `v1.3.0-staging` | staging | **다음 배포 예정**                                |

---

## Git 태그 관리

Git 태그는 특정 커밋에 이름표를 붙이는 기능입니다. 배포 시점의 코드 스냅샷을 기록하여, 나중에 "이 버전에 뭐가 포함되어 있었지?"를 확인할 수 있습니다.

### Annotated Tag vs Lightweight Tag

```bash
# Annotated Tag (권장) — 작성자, 날짜, 메시지가 포함됨
git tag -a v1.3.0-staging -m "staging 배포: 마이페이지 확장, 보안 에러 추상화"

# Lightweight Tag — 단순 포인터, 메타데이터 없음
git tag v1.3.0-staging
```

**Annotated Tag를 사용하는 것을 권장합니다.** `git show` 명령어로 태그 작성자, 날짜, 메시지를 확인할 수 있기 때문입니다.

### 주요 명령어

```bash
# ── 태그 생성 ──
# 현재 커밋에 태그
git tag -a v1.3.0-staging -m "staging 배포: 마이페이지 확장"

# 특정 커밋에 태그
git tag -a v1.3.0-staging abc1234 -m "staging 배포: 마이페이지 확장"


# ── 태그 Push ──
# 특정 태그만 push
git push origin v1.3.0-staging

# 모든 태그 push (주의: 로컬의 모든 태그가 올라감)
git push origin --tags


# ── 태그 조회 ──
# 전체 태그 목록
git tag -l

# staging 태그만 필터
git tag -l "*staging*"

# 태그 상세 정보 (커밋, 메시지, 날짜, 작성자)
git show v1.3.0-staging


# ── 태그 간 비교 ──
# 두 버전 사이 변경 커밋 목록
git log v1.1.1..v1.3.0-staging --oneline

# 두 버전 사이 코드 변경 사항
git diff v1.1.1..v1.3.0-staging --stat


# ── 태그 삭제 (실수로 잘못 태그했을 때) ──
# 로컬 삭제
git tag -d v1.3.0-staging

# 원격 삭제
git push origin --delete v1.3.0-staging
```

---

## GitHub Release 생성

GitHub Release는 Git 태그 위에 **릴리즈 노트 문서를 덧붙이는 기능**입니다. 태그만 달면 코드 스냅샷만 기록되지만, Release를 생성하면 변경 사항 설명과 함께 팀 전체에 알림이 갑니다.

### 방법 1: GitHub 웹 UI

1. 저장소 → **Releases** 탭 → **Draft a new release**
2. **Choose a tag**: 기존 태그 선택 또는 새 태그 입력
3. **Target branch**: `staging` 또는 `prod` 선택
4. **Release title**: `v1.3.0-staging - 마이페이지 확장, 보안 에러 추상화`
5. **Description**: 릴리즈 노트 내용 붙여넣기
6. staging 배포의 경우 **"Set as a pre-release"** 체크
7. **Publish release**

> **"Generate release notes"** 버튼을 누르면 이전 태그 이후의 PR 목록을 자동으로 생성해줍니다. 이걸 기반으로 릴리즈 노트를 정리하면 편리합니다.

### 방법 2: GitHub CLI (`gh`)

```bash
# staging 릴리즈 (pre-release로 표시)
gh release create v1.3.0-staging \
  --title "v1.3.0-staging - 마이페이지 확장, 보안 에러 추상화" \
  --notes-file docs/release-notes/v1.3.0-staging.md \
  --target staging \
  --prerelease

# prod 릴리즈 (정식 릴리즈)
gh release create v1.3.0 \
  --title "v1.3.0 - 마이페이지 확장, 보안 에러 추상화" \
  --notes-file docs/release-notes/v1.3.0.md \
  --target prod

# 자동 생성된 릴리즈 노트 기반으로 생성
gh release create v1.3.0-staging \
  --title "v1.3.0-staging" \
  --generate-notes \
  --target staging \
  --prerelease
```

### Pre-release vs Release

| 구분          | 용도                   | GitHub 표시            |
|-------------|----------------------|----------------------|
| Pre-release | staging 배포 (QA 진행 중) | 노란색 `Pre-release` 배지 |
| Release     | prod 배포 (정식 릴리즈)     | 초록색 `Latest` 배지      |

---

## 전체 배포 + 릴리즈 워크플로우

```
1. 변경 사항 확인
   git log v1.1.1..dev --oneline

2. dev → staging 머지
   git checkout staging
   git merge dev

3. 태그 생성 + push
   git tag -a v1.3.0-staging -m "staging 배포: 마이페이지 확장"
   git push origin staging
   git push origin v1.3.0-staging

4. 릴리즈 노트 작성
   docs/release-notes/v1.3.0-staging.md 에 작성

5. GitHub Release 생성
   gh release create v1.3.0-staging --notes-file ... --prerelease

6. 노션에 릴리즈 노트 복사 + GitHub Release URL 링크

7. 기획팀/클라우드팀에 공유
```

---

## 릴리즈 노트 템플릿

아래 템플릿을 복사하여 **노션**, **GitHub Release**, **docs/release-notes/** 디렉터리에서 사용합니다.

```markdown
# vX.Y.Z-staging (YYYY-MM-DD)

> **배포 환경**: staging
> **이전 버전**: vX.Y.Z-staging
> **배포 브랜치**: dev → staging
> **배포 담당자**: @이름

---

## 주요 변경 사항

이번 릴리즈의 핵심을 1~2문장으로 요약합니다.

---

## Feature (새로운 기능)

| 모듈 | 내용 | 이슈 |
|------|------|------|
| module | 설명 | GRGB-000 |

## Fixed (버그 수정)

| 모듈 | 내용 | 이슈 |
|------|------|------|
| module | 설명 | GRGB-000 |

## Improved (개선)

| 모듈 | 내용 | 이슈 |
|------|------|------|
| module | 설명 | GRGB-000 |

## Hotfix (긴급 수정)

| 모듈 | 내용 | 이슈 |
|------|------|------|
| module | 설명 | GRGB-000 |

## Infra (인프라 변경)

| 내용 | 이슈 |
|------|------|
| 설명 | - |

---

## 기획/클라우드팀 참고 사항

- 변경되는 API 엔드포인트
- 필요한 환경변수 추가/변경
- DB 마이그레이션 필요 여부

## 알려진 이슈

- 해당 사항 없음

---

<details>
<summary>전체 커밋 목록</summary>

`git log vPREV..vCURR --oneline` 결과

</details>
```

---

## 실제 작성 예시

> 아래는 v1.1.1 이후 dev 브랜치에 쌓인 변경 사항을 기반으로 한 예시입니다.

```markdown
# v1.3.0-staging (2026-03-30)

> **배포 환경**: staging
> **이전 버전**: v1.1.1
> **배포 브랜치**: dev → staging
> **배포 담당자**: @Seulgi

---

## 주요 변경 사항

마이페이지 기능 확장(계정 조회/수정, 티켓 취소, 입장 QR), 보안 강화(에러 메시지 추상화, DB 암호화), 좌석 추천 로직 개선

---

## Feature

| 모듈 | 내용 | 이슈 |
|------|------|------|
| mypage | 유저 계정 정보 조회 API | GRGB-314 |
| user-account | 유저 닉네임 수정 API | GRGB-312 |
| mypage | 티켓 취소 요청 API | GRGB-302 |
| mypage | 입장 QR 조회 API | - |
| auth-guard | AI 서버 전용 Internal API Key 인증 필터 | - |
| auth-guard | 유저 차단/차단해제 API | GRGB-300 |
| common-core | DB 개인정보 AES-256-GCM 양방향 암호화 | GRGB-305 |
| common-core | 프로필별 에러 응답 코드 추상화 전략 | - |

## Fixed

| 모듈 | 내용 | 이슈 |
|------|------|------|
| order | 결제 완료 시 match_seat SOLD 상태 전환 누락 수정 | GRGB-318 |
| seat | 일반 좌석 선점 시 티켓 수 비교 로직 삭제 | GRGB-308 |
| seat | 추천 블록 조회 시 Block PK 대신 blockNum으로 조회하도록 수정 | GRGB-303 |
| logging | HTTP 요청 로그 미출력 문제 해결 | - |
| metric | 메트릭 버킷 내보내기 설정 추가 | GRGB-304 |

## Improved

| 모듈 | 내용 | 이슈 |
|------|------|------|
| seat | 블럭 추천 리스트에 준연석 포도알 카운팅 반영 및 최대 티켓 수 변경 | GRGB-315 |
| error | 보안 정책에 따라 에러 메시지 추상화 | GRGB-316 |
| mypage | MyPageService 역할에 따른 분리 | - |

## Hotfix

| 모듈 | 내용 | 이슈 |
|------|------|------|
| auth-guard | 부하테스트 회원가입 동시 요청 시 중복 500 → 409 응답 처리 | - |

## Infra

| 내용 | 이슈 |
|------|------|
| docker-compose 환경변수 추가 | - |
```

---

## 노션 릴리즈 노트 관리

### 추천 구조: 노션 데이터베이스

| 속성             | 타입     | 예시                                                |
|----------------|--------|---------------------------------------------------|
| 버전             | Title  | v1.3.0-staging                                    |
| 배포일            | Date   | 2026-03-30                                        |
| 환경             | Select | staging / prod                                    |
| 상태             | Select | 배포 완료 / 롤백                                        |
| GitHub Release | URL    | https://github.com/...releases/tag/v1.3.0-staging |
| 담당자            | Person | @Seulgi                                           |

### 활용 방법

1. 위 마크다운 템플릿을 노션 페이지에 **그대로 붙여넣기**하면 표가 자동 변환됩니다.
2. GitHub Release URL을 속성에 링크해두면 **코드 레벨 추적**이 가능합니다.
3. 기획팀/클라우드팀은 노션 데이터베이스에서 **환경별 필터**로 원하는 릴리즈만 확인할 수 있습니다.
4. 각 릴리즈 노트 페이지 내에서 기획팀/클라우드팀이 **코멘트**를 남기면 소통이 편리합니다.

### 참고 서비스 릴리즈 노트

| 서비스                                                                        | 특징                                            | 참고         |
|----------------------------------------------------------------------------|-----------------------------------------------|------------|
| [NHN Cloud](https://docs.nhncloud.com/ko/Game/Launching/ko/release-notes/) | 날짜 기준, `[기능 추가]` `[버그 수정]` 태그, 간결한 설명         | 가장 심플한 형태  |
| [토스페이먼츠](https://docs.tosspayments.com/resources/release-note)             | 월별 그룹화, 상세한 설명 + 연동 가이드 링크                    | 외부 공개용에 적합 |
| [Kakao i](https://docs.kakaoi.ai/view_template/introduction/)              | 모듈별 + 버전별 표 형식, Feature/Updated/Deprecated 태그 | 개발자 문서에 적합 |
| [Netflix](https://help.netflix.com/ko/node/119359)                         | 사용자 친화적 설명, 기능 위주                             | B2C 서비스 참고 |
