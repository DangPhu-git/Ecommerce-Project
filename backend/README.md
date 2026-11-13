# Backend — Spring Boot 3.x + Java 21

Xem chi tiết architecture: [`../../docs/03-backend-architecture.md`](../../docs/03-backend-architecture.md)

## Setup

```bash
# 1. Khởi động PostgreSQL (từ repo root)
docker-compose up -d postgres

# 2. Tạo config file
cp src/main/resources/application-dev.yml.example src/main/resources/application-dev.yml
# Điền các biến môi trường cần thiết

# 3. Run
mvn spring-boot:run -Dspring.profiles.active=dev
```

## Endpoints

| URL | Mô tả |
|-----|-------|
| `http://localhost:8080/api/v1` | Base API URL |
| `http://localhost:8080/swagger-ui.html` | Swagger UI |
| `http://localhost:8080/actuator/health` | Health check |

## Environment Variables (application-dev.yml)

```yaml
# Không commit file này — xem .gitignore
spring:
  datasource:
    url: jdbc:postgresql://localhost:5432/ecommerce_dev
    username: dev
    password: dev

app:
  jwt:
    secret: your-secret-key-min-32-chars

stripe:
  api-key: sk_test_...
  webhook-secret: whsec_...
```

## Database Migrations

Flyway tự động chạy migrations khi start:
```
src/main/resources/db/migration/
├── V1__init_schema.sql
└── V2__...sql
```
