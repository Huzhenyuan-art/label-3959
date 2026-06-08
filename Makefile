# ==========================================
# MyBatis Plus Demo - Project Makefile
# ==========================================

SHELL := /bin/bash
.DEFAULT_GOAL := help

# ---------- 环境变量 ----------
ENV_FILE := .env
ENV_EXAMPLE := .env.example
COMPOSE_FILE := docker-compose.yml
COMPOSE := docker compose

# ---------- 颜色定义 ----------
RED    := \033[0;31m
GREEN  := \033[0;32m
YELLOW := \033[1;33m
BLUE   := \033[0;34m
NC     := \033[0m

# ==========================================
# 帮助信息
# ==========================================
.PHONY: help
help: ## 显示帮助信息
	@echo -e "${BLUE}MyBatis Plus Demo - 项目管理 Makefile${NC}"
	@echo ""
	@echo -e "${YELLOW}使用方法:${NC}"
	@echo -e "  make <target>"
	@echo ""
	@echo -e "${YELLOW}可用目标:${NC}"
	@grep -E '^[a-zA-Z_-]+:.*?## .*$$' $(MAKEFILE_LIST) | sort | awk 'BEGIN {FS = ":.*?## "}; {printf "  ${GREEN}%-25s${NC} %s\n", $$1, $$2}'

# ==========================================
# 环境初始化
# ==========================================
.PHONY: init
init: ## 初始化项目环境（检查依赖、创建 .env）
	@echo -e "${BLUE}=== 初始化项目环境 ===${NC}"
	@echo ""
	
	@echo -e "${YELLOW}1. 检查 Docker 环境...${NC}"
	@command -v docker >/dev/null 2>&1 || { echo -e "${RED}错误: 未安装 Docker${NC}"; exit 1; }
	@command -v docker >/dev/null 2>&1 && docker --version | awk '{print "  ✓ Docker: " $$3}'
	
	@command -v docker >/dev/null 2>&1 && docker compose version >/dev/null 2>&1 || { echo -e "${RED}错误: 未安装 Docker Compose${NC}"; exit 1; }
	@command -v docker >/dev/null 2>&1 && docker compose version | awk '{print "  ✓ Docker Compose: " $$4}'

	@echo ""
	@echo -e "${YELLOW}2. 检查 .env 文件...${NC}"
	@if [ ! -f $(ENV_FILE) ]; then \
		echo -e "${YELLOW}  .env 文件不存在，从 .env.example 复制...${NC}"; \
		cp $(ENV_EXAMPLE) $(ENV_FILE); \
		echo -e "${GREEN}  ✓ 已创建 .env 文件，请根据需要修改敏感配置${NC}"; \
	else \
		echo -e "${GREEN}  ✓ .env 文件已存在${NC}"; \
	fi

	@echo ""
	@echo -e "${YELLOW}3. 创建 Docker 缓存目录...${NC}"
	@mkdir -p .docker-cache
	@echo -e "${GREEN}  ✓ .docker-cache 目录已创建${NC}"

	@echo ""
	@echo -e "${GREEN}=== 初始化完成 ===${NC}"
	@echo ""
	@echo -e "${YELLOW}下一步操作:${NC}"
	@echo -e "  1. 编辑 .env 文件，修改敏感配置（数据库密码、JWT 密钥等）"
	@echo -e "  2. 运行 'make up' 启动所有服务"

.PHONY: check-env
check-env: ## 检查 .env 文件是否存在
	@if [ ! -f $(ENV_FILE) ]; then \
		echo -e "${RED}错误: .env 文件不存在${NC}"; \
		echo -e "${YELLOW}请先运行 'make init'${NC}"; \
		exit 1; \
	fi

# ==========================================
# 本地开发
# ==========================================
.PHONY: backend-dev
backend-dev: ## 启动后端开发服务（本地运行，非 Docker）
	@echo -e "${BLUE}=== 启动后端开发服务 ===${NC}"
	cd backend && mvn spring-boot:run -Pdev -Dspring-boot.run.profiles=dev

.PHONY: backend-test
backend-test: ## 运行后端单元测试
	@echo -e "${BLUE}=== 运行后端单元测试 ===${NC}"
	cd backend && mvn test -Ptest -Dspring.profiles.active=test

.PHONY: backend-build
backend-build: ## 构建后端 Jar 包
	@echo -e "${BLUE}=== 构建后端 Jar 包 ===${NC}"
	cd backend && mvn clean package -Pprod -DskipTests

.PHONY: frontend-dev
frontend-dev: ## 启动前端开发服务
	@echo -e "${BLUE}=== 启动前端开发服务 ===${NC}"
	cd frontend && npm run dev

.PHONY: frontend-build
frontend-build: ## 构建前端静态文件
	@echo -e "${BLUE}=== 构建前端静态文件 ===${NC}"
	cd frontend && npm run build

.PHONY: frontend-install
frontend-install: ## 安装前端依赖
	@echo -e "${BLUE}=== 安装前端依赖 ===${NC}"
	cd frontend && npm install

# ==========================================
# Docker 服务管理
# ==========================================
.PHONY: up
up: check-env ## 启动所有服务（Docker Compose）
	@echo -e "${BLUE}=== 启动所有服务 ===${NC}"
	$(COMPOSE) -f $(COMPOSE_FILE) up -d --build
	@echo ""
	@echo -e "${GREEN}=== 服务启动中 ===${NC}"
	@echo -e "${YELLOW}请等待健康检查通过，查看状态: make status${NC}"

.PHONY: up-db
up-db: check-env ## 仅启动数据库服务
	@echo -e "${BLUE}=== 启动数据库服务 ===${NC}"
	$(COMPOSE) -f $(COMPOSE_FILE) up -d db

.PHONY: down
down: ## 停止并移除所有服务
	@echo -e "${BLUE}=== 停止所有服务 ===${NC}"
	$(COMPOSE) -f $(COMPOSE_FILE) down

.PHONY: down-v
down-v: ## 停止所有服务并清除数据卷（慎用！）
	@echo -e "${YELLOW}警告: 这将删除所有数据库数据！${NC}"
	@read -p "确认继续? [y/N] " -n 1 -r; \
	echo; \
	if [[ $$REPLY =~ ^[Yy]$$ ]]; then \
		$(COMPOSE) -f $(COMPOSE_FILE) down -v; \
		echo -e "${GREEN}已清除所有数据卷${NC}"; \
	else \
		echo -e "已取消"; \
	fi

.PHONY: restart
restart: check-env ## 重启所有服务
	@echo -e "${BLUE}=== 重启所有服务 ===${NC}"
	$(COMPOSE) -f $(COMPOSE_FILE) restart

.PHONY: rebuild
rebuild: check-env ## 重新构建并启动服务
	@echo -e "${BLUE}=== 重新构建并启动服务 ===${NC}"
	$(COMPOSE) -f $(COMPOSE_FILE) up -d --build --force-recreate

.PHONY: status
status: ## 查看服务状态
	@echo -e "${BLUE}=== 服务状态 ===${NC}"
	@$(COMPOSE) -f $(COMPOSE_FILE) ps -a
	@echo ""
	@echo -e "${YELLOW}健康检查:${NC}"
	@for service in db backend frontend; do \
		status=$$($(COMPOSE) -f $(COMPOSE_FILE) ps -a --format json $$service 2>/dev/null | python3 -c "import sys,json; d=json.load(sys.stdin); print(d[0]['State'] if d else 'N/A')" 2>/dev/null || echo "N/A"); \
		health=$$($(COMPOSE) -f $(COMPOSE_FILE) ps -a --format json $$service 2>/dev/null | python3 -c "import sys,json; d=json.load(sys.stdin); print(d[0]['Health'] if d else 'N/A')" 2>/dev/null || echo "N/A"); \
		echo -e "  $$service: ${GREEN}$$status${NC} / ${YELLOW}$$health${NC}"; \
	done

.PHONY: logs
logs: ## 查看所有服务日志（实时跟踪）
	$(COMPOSE) -f $(COMPOSE_FILE) logs -f

.PHONY: logs-backend
logs-backend: ## 查看后端日志
	$(COMPOSE) -f $(COMPOSE_FILE) logs -f backend

.PHONY: logs-frontend
logs-frontend: ## 查看前端日志
	$(COMPOSE) -f $(COMPOSE_FILE) logs -f frontend

.PHONY: logs-db
logs-db: ## 查看数据库日志
	$(COMPOSE) -f $(COMPOSE_FILE) logs -f db

# ==========================================
# 测试验证
# ==========================================
.PHONY: test-all
test-all: ## 运行所有测试（后端测试 + 集成验证）
	@echo -e "${BLUE}=== 运行完整测试 ===${NC}"
	@echo ""
	@echo -e "${YELLOW}1. 运行后端单元测试...${NC}"
	$(MAKE) backend-test
	@echo ""
	@echo -e "${YELLOW}2. 启动服务进行集成验证...${NC}"
	$(MAKE) up
	@echo ""
	@echo -e "${YELLOW}3. 等待服务就绪...${NC}"
	@sleep 60
	$(MAKE) health-check
	@echo ""
	@echo -e "${GREEN}=== 所有测试通过 ===${NC}"

.PHONY: health-check
health-check: ## 检查所有服务健康状态
	@echo -e "${BLUE}=== 服务健康检查 ===${NC}"
	@echo ""
	
	@echo -e "${YELLOW}检查数据库:${NC}"
	@if curl -fsS http://localhost:8959/actuator/health > /dev/null 2>&1; then \
		echo -e "  ${GREEN}✓ 后端 API 正常${NC}"; \
	else \
		echo -e "  ${RED}✗ 后端 API 不可访问${NC}"; \
		exit 1; \
	fi
	
	@echo -e "${YELLOW}检查前端:${NC}"
	@if curl -fsS http://localhost:3959/ > /dev/null 2>&1; then \
		echo -e "  ${GREEN}✓ 前端页面正常${NC}"; \
	else \
		echo -e "  ${RED}✗ 前端页面不可访问${NC}"; \
		exit 1; \
	fi
	
	@echo -e "${YELLOW}检查 Actuator 端点:${NC}"
	@for endpoint in health info; do \
		if curl -fsS http://localhost:8959/actuator/$$endpoint > /dev/null 2>&1; then \
			echo -e "  ${GREEN}✓ /actuator/$$endpoint${NC}"; \
		else \
			echo -e "  ${RED}✗ /actuator/$$endpoint${NC}"; \
		fi; \
	done
	
	@echo ""
	@echo -e "${GREEN}=== 健康检查完成 ===${NC}"

# ==========================================
# 清理
# ==========================================
.PHONY: clean
clean: ## 清理构建产物
	@echo -e "${BLUE}=== 清理构建产物 ===${NC}"
	cd backend && mvn clean
	rm -rf frontend/dist
	rm -rf backend/target
	@echo -e "${GREEN}✓ 清理完成${NC}"

.PHONY: clean-cache
clean-cache: ## 清理 Docker 构建缓存
	@echo -e "${BLUE}=== 清理 Docker 构建缓存 ===${NC}"
	rm -rf .docker-cache
	docker builder prune -f
	@echo -e "${GREEN}✓ 缓存已清理${NC}"

.PHONY: clean-all
clean-all: down clean clean-cache ## 完全清理（停止服务 + 清理产物 + 清理缓存）
	@echo -e "${GREEN}=== 完全清理完成 ===${NC}"

# ==========================================
# 快捷命令
# ==========================================
.PHONY: start
start: init up ## 初始化并启动服务（首次使用）
	@echo ""
	@echo -e "${GREEN}=== 服务启动完成 ===${NC}"
	@echo -e "${YELLOW}访问地址:${NC}"
	@echo -e "  前端: http://localhost:3959"
	@echo -e "  后端 API: http://localhost:8959/api"
	@echo -e "  数据库: localhost:33959"
	@echo -e "  Actuator: http://localhost:8959/actuator"

.PHONY: stop
stop: down ## 停止服务（别名）

.PHONY: urls
urls: ## 显示所有服务访问地址
	@echo -e "${BLUE}=== 服务访问地址 ===${NC}"
	@echo ""
	@echo -e "${GREEN}前端:${NC}       http://localhost:3959"
	@echo -e "${GREEN}后端 API:${NC}   http://localhost:8959/api"
	@echo -e "${GREEN}Actuator:${NC}   http://localhost:8959/actuator"
	@echo -e "${GREEN}数据库:${NC}     localhost:33959"
	@echo -e "${GREEN}监控端点:${NC}"
	@echo -e "  - 健康检查: http://localhost:8959/actuator/health"
	@echo -e "  - 应用信息: http://localhost:8959/actuator/info"
	@echo -e "  - 指标:     http://localhost:8959/actuator/metrics"
	@echo -e "  - Prometheus: http://localhost:8959/actuator/prometheus"
