# Docker Compose 사용 가이드

이 문서는 개발팀 및 사용자가 `docker-compose`를 사용하여 로컬 환경에서 전체 서비스를 손쉽게 실행하고 관리하는 방법을 안내합니다.

## 📋 목차
- [사전 요구 사항](#사전-요구-사항)
- [설정 및 실행](#설정-및-실행)
- [주요 명령어](#주요-명령어)
- [서비스 접속 정보](#서비스-접속-정보)
- [API 문서 (Swagger UI)](#api-문서-swagger-ui)
- [문제 해결 (Troubleshooting)](#문제-해결-troubleshooting)

---

## 사전 요구 사항
1. **Docker Desktop 설치**
   - [Docker 공식 홈페이지](https://www.docker.com/products/docker-desktop/)에서 OS에 맞는 버전을 설치해주세요.
   - 터미널에서 `docker -v`, `docker-compose -v` 명령어로 설치 여부를 확인할 수 있습니다.

2. **환경 변수 파일 (.env) 준비**
   - 프로젝트 루트 디렉토리에 `.env` 파일이 필요합니다.
   - `.env.example` 파일이 있다면 복사하여 `.env`를 생성하고, 필요한 값(Secret Key 등)을 채워주세요.

---

## 설정 및 실행

### 1단계: 서비스 시작

터미널(프로젝트 루트 경로)에서 실행하려는 환경에 맞춰 아래 명령어를 실행하세요.

**기본 실행 (Default)**
```bash
docker-compose up --build -d
```

**로컬 환경 실행 (Local Profile)**
```bash
docker-compose -f docker-compose.yml -f docker-compose.local.yml up --build -d
```
> **설명**: `local` 프로필이 적용되어 로컬 개발 설정으로 실행됩니다.

**개발 환경 실행 (Dev Profile)**
```bash
docker-compose -f docker-compose.yml -f docker-compose.dev.yml up --build -d
```
> **설명**: `dev` 프로필이 적용되어 개발 서버 설정으로 실행됩니다.

> **Tip**: 처음 실행 시 이미지를 다운로드하고 빌드하느라 시간이 소요될 수 있습니다.

---

## 주요 명령어

| 동작 | 명령어 | 설명 |
| --- | --- | --- |
| **실행** | `docker-compose up -d` | 서비스를 백그라운드에서 실행합니다. |
| **빌드 후 실행** | `docker-compose up --build -d` | 코드 변경 사항을 반영하여 이미지를 재빌드하고 실행합니다. |
| **로그 확인 (전체)** | `docker-compose logs -f` | 모든 컨테이너의 로그를 실시간으로 확인합니다. |
| **로그 확인 (개별)** | `docker-compose logs -f [서비스명]` | 특정 서비스(예: `auth-guard`)의 로그만 확인합니다. |
| **상태 확인** | `docker-compose ps` | 현재 실행 중인 컨테이너 목록과 상태를 조회합니다. |
| **중지** | `docker-compose stop` | 컨테이너를 중지합니다. (제거하지 않음) |
| **종료 및 제거** | `docker-compose down` | 컨테이너와 네트워크를 종료하고 제거합니다. |
| **데이터 초기화 포함 종료** | `docker-compose down -v` | **주의**: DB 데이터 볼륨까지 모두 삭제합니다. |

---

## 서비스 접속 정보

로컬 환경(`localhost`)에서 각 서비스는 아래 포트로 매핑됩니다.

| 서비스 명 | 컨테이너 명 | 포트 | 설명 |
| --- | --- | --- | --- |
| **Auth-Guard** | `auth-guard` | **8080** | 인증/인가 게이트웨이 & 메인 엔트리포인트 |
| **Queue** | `queue` | **8081** | 대기열 관리 서비스 |
| **Seat** | `seat` | **8082** | 좌석 예약 서비스 |
| **Order-Core** | `order-core` | **8083** | 주문 처리 서비스 |
| **Recommendation** | `recommendation` | **8084** | 추천 서비스 |
| **PostgreSQL** | `goormgb-postgres` | **5432** | 메인 데이터베이스 |
| **Redis** | `goormgb-redis` | **6379** | 캐시 및 세션 저장소 |

---
## 문제 해결 (Troubleshooting)

### DB 연결 오류 (Connection Refused)
- **현상**: 서비스 로그에 `Connection refused` 또는 `Cannot connect to database` 에러 발생.
- **원인**: Database 컨테이너가 완전히 구동되기 전에 애플리케이션 컨테이너가 먼저 실행되어 연결을 시도했기 때문입니다.
- **해결**: `docker-compose.yml`에 `healthcheck`가 설정되어 있어 자동으로 재시도합니다. 잠시 기다리면 정상적으로 연결됩니다.

### 포트 충돌 (Bind for 0.0.0.0:xxxx failed)
- **현상**: `Error starting userland proxy: listen tcp4 0.0.0.0:xxx: bind: address already in use` 에러 발생.
- **원인**: 로컬 컴퓨터에서 이미 해당 포트(5432, 6379, 8080 등)를 다른 프로세스가 사용 중입니다.
- **해결**: 해당 포트를 사용하는 프로세스를 종료하거나, `docker-compose.yml`에서 호스트 포트 매핑을 변경해야 합니다.

### 빌드 오류 (FileNotFound 등)
- **현상**: `COPY ... failed: file not found ...`
- **해결**: `docker-compose build` 시 빌드 컨텍스트가 프로젝트 루트(`.`)로 설정되어 있는지 확인하세요. (현재 설정은 이미 적용되어 있습니다.)
