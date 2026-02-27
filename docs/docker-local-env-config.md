# Docker 환경과 로컬(IntelliJ) 환경의 설정 차이

## 배경

API Gateway 도입 후, Docker 환경과 IntelliJ 로컬 환경에서 동일한 `local` 프로필을 사용하되 네트워크 설정만 다르게 동작해야 하는 요구사항이 생겼습니다.

## 핵심 문제: Docker vs IntelliJ 네트워크 차이

| 환경 | 네트워크 | `localhost`의 의미 |
|------|----------|-------------------|
| IntelliJ | 모든 서비스가 같은 머신에서 실행 | 다른 서비스에 접근 가능 |
| Docker | 각 서비스가 별도 컨테이너(별도 네트워크) | **자기 자신 컨테이너만 가리킴** |

예를 들어 Gateway 컨테이너에서 `http://localhost:8083`으로 요청하면, Order-Core가 아니라 **Gateway 자기 자신의 8083 포트**로 요청하게 되어 `Connection refused` 에러가 발생합니다.

## 해결 방법: `${ENV_VAR:기본값}` 패턴

### 변경 전 (application-local.yaml)

```yaml
routes:
  - id: order-core
    uri: http://localhost:8083    # 하드코딩 → Docker에서 안 됨
    predicates:
      - Path=/order/**
```

### 변경 후 (application-local.yaml)

```yaml
routes:
  - id: order-core
    uri: ${ORDER_CORE_URL:http://localhost:8083}    # 환경변수 + 기본값
    predicates:
      - Path=/order/**
```

### 동작 방식

| 환경 | `ORDER_CORE_URL` 환경변수 | 사용되는 값 |
|------|--------------------------|-----------|
| IntelliJ | 없음 (미설정) | 기본값 `http://localhost:8083` |
| Docker | `http://order-core:8083` (docker-compose.yml에서 주입) | `http://order-core:8083` |

Spring의 `${VAR:default}` 문법 덕분에 하나의 yaml 파일로 두 환경을 모두 커버할 수 있습니다.

## 변경된 파일 목록

### 1. API-Gateway/src/main/resources/application-local.yaml

라우팅 URI를 환경변수 + 기본값 패턴으로 변경:

```yaml
routes:
  - id: auth-guard
    uri: ${AUTH_GUARD_URL:http://localhost:8080}
  - id: queue
    uri: ${QUEUE_URL:http://localhost:8081}
  - id: seat
    uri: ${SEAT_URL:http://localhost:8082}
  - id: order-core
    uri: ${ORDER_CORE_URL:http://localhost:8083}
  - id: recommendation
    uri: ${RECOMMENDATION_URL:http://localhost:8084}
```

### 2. .env (프로젝트 루트)

Docker 실행 시 `--env-file ../.env`로 주입되는 환경변수에 Gateway 라우팅 URL 추가:

```env
# === API Gateway ===
ALLOWED_ORIGINS=http://localhost:*
AUTH_GUARD_URL=http://auth-guard:8080
QUEUE_URL=http://queue:8081
SEAT_URL=http://seat:8082
ORDER_CORE_URL=http://order-core:8083
RECOMMENDATION_URL=http://recommendation:8084
```

`http://auth-guard:8080`에서 `auth-guard`는 Docker Compose의 **서비스 이름(= 컨테이너 DNS 이름)**입니다.

### 3. 나머지 downstream 서비스 (Auth-Guard, Queue, Seat, Order-Core, Recommendation)

이들은 변경 불필요. 이유:
- DB, Redis 접속 정보는 이미 `${DB_URL:jdbc:postgresql://localhost:5432/goormgb}` 패턴으로 되어 있음
- Docker 환경에서 `docker-compose.yml`의 `environment`로 덮어쓰기됨
- 이 서비스들은 다른 서비스로 라우팅하지 않으므로 localhost 문제가 발생하지 않음

## Docker 실행 명령어

```bash
cd docker

# local 프로필
docker compose --env-file ../.env -f docker-compose.yml -f docker-compose.local.yml up --build -d

# dev 프로필
docker compose --env-file ../.env -f docker-compose.yml -f docker-compose.dev.yml up --build -d

# default 프로필 (프로필 오버라이드 없이)
docker compose --env-file ../.env -f docker-compose.yml up --build -d
```

## 주의사항

- `.env` 파일은 `.gitignore`에 포함되어 있으므로 팀원과 공유 시 별도 전달 필요 (구름공방 알림디코 채널의 -> backend 카테고리 -> 설정-환경변수env 채팅방으로 공유)
- Docker 컨테이너는 Spring Boot 기동에 시간이 걸리므로 (약 15~30초), Gateway Swagger UI 접속 전 모든 서비스가 기동 완료될 때까지 대기 필요
