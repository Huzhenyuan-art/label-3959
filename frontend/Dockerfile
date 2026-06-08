# syntax=docker/dockerfile:1.6

# ============================
# 阶段 1: 依赖缓存
# ============================
FROM node:20.15.0-alpine AS deps
WORKDIR /app

COPY package.json ./
RUN --mount=type=cache,target=/root/.npm \
    npm config set registry https://registry.npmmirror.com \
    && npm install --omit=optional --no-audit --no-fund

# ============================
# 阶段 2: 应用构建
# ============================
FROM node:20.15.0-alpine AS builder
WORKDIR /app

ARG BUILD_MODE=production
ENV NODE_ENV=production

COPY --from=deps /app/node_modules ./node_modules
COPY . .

RUN --mount=type=cache,target=/root/.npm \
    if [ "$BUILD_MODE" = "development" ]; then \
      npm run build:dev; \
    elif [ "$BUILD_MODE" = "test" ]; then \
      npm run build:test; \
    else \
      npm run build; \
    fi

# ============================
# 阶段 3: 运行时镜像
# ============================
FROM nginx:1.27.0-alpine AS runtime

ENV TZ=Asia/Shanghai

RUN apk add --no-cache \
    curl \
    tzdata \
    && ln -sf /usr/share/zoneinfo/${TZ} /etc/localtime \
    && echo "${TZ}" > /etc/timezone \
    && rm -rf /usr/share/nginx/html/* \
    && mkdir -p /var/cache/nginx \
    && chown -R nginx:nginx /var/cache/nginx /var/run /var/log/nginx

COPY --from=builder /app/dist /usr/share/nginx/html
COPY nginx.conf /etc/nginx/conf.d/default.conf

RUN sed -i 's/user nginx;/user nginx nginx;/' /etc/nginx/nginx.conf \
    && chown -R nginx:nginx /usr/share/nginx/html /etc/nginx/conf.d

HEALTHCHECK --interval=30s --timeout=5s --retries=3 --start-period=10s \
    CMD curl -f http://localhost/ || exit 1

EXPOSE 80

USER nginx

CMD ["nginx", "-g", "daemon off;"]

