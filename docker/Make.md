# Docker 명령어 가이드 (Make)

## 사전 준비

1. Docker Desktop 설치
2. `.env` 파일 준비 (루트 디렉토리)

## 명령어

```
bash
cd docker

| 명령어                     | 설명               |
|----------------------------|--------------------|
| make help                  | 전체 명령어 목록   |
| make local-build           | 빌드 & 실행        |
| make local-up              | 실행 (빌드 없이)   |
| make local-down            | 종료               |
| make local-clean           | 종료 + DB 초기화   |
| make local-restart         | 재시작             |
| make local-ps              | 상태 확인          |
| make local-logs            | 전체 로그          |
| make local-logs-auth-guard | 특정 서비스 로그   |
| make local-infra           | DB + Redis만 실행  |
| make local-only-auth-guard | 특정 서비스만 실행 |
| make local-rebuild-queue   | 특정 서비스 재빌드 |

서비스 포트

| 서비스         | 포트 |
|----------------|------|
| Auth-Guard     | 8080 |
| Queue          | 8081 |
| Seat           | 8082 |
| Order-Core     | 8083 |
| Recommendation | 8084 |
| PostgreSQL     | 5432 |
| Redis          | 6379 |

문제 해결

포트 충돌: 해당 포트 사용 중인 프로세스 종료
DB 초기화: make local-clean 후 make local-build
```
