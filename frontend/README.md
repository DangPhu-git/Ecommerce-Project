# Frontend — React + Vite + TypeScript

Xem chi tiết architecture: [`../../docs/02-frontend-architecture.md`](../../docs/02-frontend-architecture.md)

## Setup

```bash
pnpm install
cp .env.example .env.local
pnpm dev
```

## Scripts

```bash
pnpm dev        # Development server (http://localhost:5173)
pnpm build      # Production build
pnpm preview    # Preview production build
pnpm tsc        # Type check
pnpm lint       # ESLint
pnpm test       # Vitest unit tests
```

## Environment Variables

```bash
# .env.local (không commit — xem .gitignore)
VITE_API_URL=http://localhost:8080
VITE_STRIPE_PUBLISHABLE_KEY=pk_test_...
```
