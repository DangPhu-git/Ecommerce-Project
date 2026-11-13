# Frontend — React + Vite + TypeScript

Xem chi tiết architecture: [`../../docs/02-frontend-architecture.md`](../../docs/02-frontend-architecture.md)

## Setup

```bash
npm install
cp .env.example .env.local
npm run dev
```

## Scripts

```bash
npm run dev       # Development server (http://localhost:5173)
npm run build     # Production build
npm run preview   # Preview production build
npm run tsc       # Type check
npm run lint      # ESLint
npm test          # Vitest unit tests
```

## Environment Variables

```bash
# .env.local (không commit — xem .gitignore)
VITE_API_URL=http://localhost:8080
VITE_STRIPE_PUBLISHABLE_KEY=pk_test_...
```

