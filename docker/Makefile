# Docker commands for goormgb-backend

COMPOSE = docker-compose --env-file ../.env -f docker-compose.yml
LOCAL = $(COMPOSE) -f docker-compose.local.yml
DEV = $(COMPOSE) -f docker-compose.dev.yml

.PHONY: help local-build local-up local-down local-clean local-restart local-ps local-logs local-infra local-prune local-reset

help:
	@echo "=== Local Development ==="
	@echo "  make local-build            전체 빌드 & 실행"
	@echo "  make local-up               전체 실행"
	@echo "  make local-down             종료"
	@echo "  make local-clean            종료 + DB 초기화"
	@echo "  make local-restart          재시작"
	@echo "  make local-ps               상태 확인"
	@echo "  make local-logs             전체 로그"
	@echo "  make local-infra            DB + Redis만"
	@echo "  make local-prune            컨테이너 강제 삭제"
	@echo "  make local-reset            정리 후 재빌드"
	@echo ""
	@echo "  make local-only-auth-guard  특정 서비스만"
	@echo "  make local-rebuild-queue    특정 서비스 재빌드"

# === Local ===
local-build:
	$(LOCAL) up --build -d

local-up:
	$(LOCAL) up -d

local-down:
	$(COMPOSE) down

local-restart: 
	local-down local-up

local-ps:
	$(COMPOSE) ps

local-logs:
	$(COMPOSE) logs -f

local-logs-%:
	$(COMPOSE) logs -f $*

local-infra:
	$(COMPOSE) up -d local-postgres local-redis

local-only-%:
	$(LOCAL) up -d local-postgres local-redis $*

local-rebuild-%:
	$(LOCAL) up --build -d $*

# === 정리 ===
local-clean:
	$(COMPOSE) down -v

local-prune:
	-docker rm -f goormgb-postgres goormgb-redis 2>/dev/null
	-docker container prune -f

local-reset: local-prune local-build
