# 🛒 Ecommerce Platform

> **Stack**: React 18 + Vite (FE) · Spring Boot 3.x / Java 21 (BE) · PostgreSQL on Neon (DB)

---

## 📁 Repository Structure

```
Ecommerce-Project/          ← Git root (đây là repo)
├── frontend/               # React + Vite + TypeScript
│   ├── src/
│   ├── package.json
│   └── vite.config.ts
├── backend/                # Spring Boot 3.x + Java 21
│   ├── src/
│   └── pom.xml
├── .github/
│   └── workflows/          # GitHub Actions CI/CD
├── docker-compose.yml      # Local development
├── .gitignore
└── README.md
```

> **Note**: Thư mục `docs/` (architecture documentation) nằm **bên ngoài** repo này tại  
> `../docs/` — không được commit vào Git.

---

## 🚀 Quick Start

### Prerequisites
- Node.js 20+, pnpm 9+
- Java 21 (JDK)
- Maven 3.9+
- Docker Desktop (cho local PostgreSQL)

### Frontend
```bash
cd frontend
pnpm install
cp .env.example .env.local
# Điền VITE_API_URL và VITE_STRIPE_PUBLISHABLE_KEY
pnpm dev
# → http://localhost:5173
```

### Backend
```bash
# Khởi động PostgreSQL local
docker-compose up -d postgres

cd backend
# Tạo application-dev.yml từ template
cp src/main/resources/application-dev.yml.example src/main/resources/application-dev.yml
# Điền DATABASE_URL, JWT_SECRET, STRIPE_SECRET_KEY

mvn spring-boot:run -Dspring.profiles.active=dev
# → http://localhost:8080
# → Swagger UI: http://localhost:8080/swagger-ui.html
```

---

## 🌿 Git Workflow

Dự án dùng **Trunk-Based Development**:
- Branch ngắn từ `main` (< 2-3 ngày)
- Conventional Commits: `feat:`, `fix:`, `chore:`, `docs:`, `refactor:`, `test:`
- PR phải pass CI trước khi merge
- Squash merge vào `main`

Xem chi tiết: [`../docs/07-git-workflow.md`](../docs/07-git-workflow.md)

---

## 📚 Architecture Docs

Tài liệu kiến trúc đầy đủ ở `../docs/` (ngoài repo):

| File | Nội dung |
|------|----------|
| `00-overview.md` | Tổng quan hệ thống |
| `01-system-architecture.md` | System design |
| `02-frontend-architecture.md` | React architecture |
| `03-backend-architecture.md` | Spring Boot architecture |
| `04-database-architecture.md` | PostgreSQL schema |
| `05-database-reliability.md` | HA & backup strategy |
| `06-payments-architecture.md` | Stripe integration |
| `07-git-workflow.md` | Git workflow |
| `08-ui-design-system.md` | UI design system |

---

## 🔗 Key Endpoints (Local)

| Service | URL |
|---------|-----|
| Frontend | http://localhost:5173 |
| Backend API | http://localhost:8080/api/v1 |
| Swagger UI | http://localhost:8080/swagger-ui.html |
| Health Check | http://localhost:8080/actuator/health |
