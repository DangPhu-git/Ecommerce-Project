import { useState, useEffect } from 'react';
import {
  Button,
  Input,
  Card,
  CardHeader,
  CardTitle,
  CardDescription,
  CardContent,
  CardFooter,
  Modal,
  Badge,
  Skeleton,
} from '@/components/ui';
import {
  Sun,
  Moon,
  ShoppingCart,
  Search,
  CheckCircle,
  AlertTriangle,
  Layers,
  ArrowRight,
  ShieldCheck,
  Laptop,
} from 'lucide-react';

export default function App() {
  const [theme, setTheme] = useState<'light' | 'dark'>('light');
  const [isModalOpen, setIsModalOpen] = useState(false);
  const [inputValue, setInputValue] = useState('');
  const [inputError, setInputError] = useState('');
  const [buttonLoading, setButtonLoading] = useState(false);

  useEffect(() => {
    const root = document.documentElement;
    root.setAttribute('data-theme', theme);
    if (theme === 'dark') {
      root.classList.add('dark');
    } else {
      root.classList.remove('dark');
    }
  }, [theme]);

  const toggleTheme = () => {
    setTheme((prev) => (prev === 'light' ? 'dark' : 'light'));
  };

  const handleSimulateLoad = () => {
    setButtonLoading(true);
    setTimeout(() => {
      setButtonLoading(false);
    }, 1500);
  };

  return (
    <div className="min-h-screen bg-bg-base text-text-primary transition-colors duration-200">
      {/* Top Navigation */}
      <header className="sticky top-0 z-40 border-b border-border-light bg-bg-surface/80 backdrop-blur-md">
        <div className="mx-auto flex max-w-7xl items-center justify-between px-4 py-4 sm:px-6 lg:px-8">
          <div className="flex items-center gap-3">
            <div className="flex h-10 w-10 items-center justify-center rounded-lg bg-primary-500 text-white shadow-sm">
              <ShoppingCart className="h-5 w-5" />
            </div>
            <div>
              <span className="text-xl font-bold tracking-tight text-text-primary">
                Vibe<span className="text-primary-500">Commerce</span>
              </span>
              <Badge variant="secondary" className="ml-2">
                Phase 0: Scaffolding
              </Badge>
            </div>
          </div>

          <div className="flex items-center gap-3">
            <Button
              variant="outline"
              size="sm"
              onClick={toggleTheme}
              aria-label="Toggle Theme"
              leftIcon={
                theme === 'light' ? (
                  <Moon className="h-4 w-4" />
                ) : (
                  <Sun className="h-4 w-4" />
                )
              }
            >
              {theme === 'light' ? 'Dark Mode' : 'Light Mode'}
            </Button>
            <Button
              variant="primary"
              size="sm"
              leftIcon={<ShieldCheck className="h-4 w-4" />}
            >
              CI Ready
            </Button>
          </div>
        </div>
      </header>

      {/* Main Content / Showcase */}
      <main className="mx-auto max-w-7xl px-4 py-8 sm:px-6 lg:px-8 space-y-10">
        {/* Hero Section */}
        <section className="rounded-2xl border border-border-light bg-gradient-to-r from-primary-50 to-primary-100/50 dark:from-secondary-900 dark:to-secondary-800 p-8 sm:p-10">
          <div className="max-w-2xl space-y-4">
            <Badge variant="default" className="text-xs">
              Baseline Initialized
            </Badge>
            <h1 className="text-3xl font-extrabold tracking-tight sm:text-4xl text-text-primary">
              Ecommerce Platform — Giai Đoạn 0
            </h1>
            <p className="text-base text-text-secondary leading-relaxed">
              Khung dự án chuẩn Modular Monolith (Spring Boot 3.3 + Java 21) & Frontend (React 18 + Vite + TypeScript + Tailwind CSS). Bộ Design Tokens và Base UI Component Kit đã sẵn sàng cho Giai đoạn 1.
            </p>
            <div className="flex flex-wrap gap-3 pt-2">
              <Button
                variant="primary"
                onClick={() => setIsModalOpen(true)}
                rightIcon={<ArrowRight className="h-4 w-4" />}
              >
                Mở Modal Demo
              </Button>
              <Button
                variant="secondary"
                isLoading={buttonLoading}
                onClick={handleSimulateLoad}
              >
                Simulate Loading State
              </Button>
            </div>
          </div>
        </section>

        {/* Component Showcase Grid */}
        <section className="space-y-6">
          <div className="flex items-center gap-2">
            <Layers className="h-5 w-5 text-primary-500" />
            <h2 className="text-xl font-bold tracking-tight text-text-primary">
              Base UI Component Kit & Design Tokens
            </h2>
          </div>

          <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6">
            {/* 1. Buttons Card */}
            <Card>
              <CardHeader>
                <CardTitle>Button Variants & States</CardTitle>
                <CardDescription>
                  Đạt chuẩn WCAG AA với focus rings và states
                </CardDescription>
              </CardHeader>
              <CardContent className="space-y-3">
                <div className="flex flex-wrap gap-2">
                  <Button variant="primary" size="sm">Primary</Button>
                  <Button variant="secondary" size="sm">Secondary</Button>
                  <Button variant="outline" size="sm">Outline</Button>
                  <Button variant="ghost" size="sm">Ghost</Button>
                  <Button variant="danger" size="sm">Danger</Button>
                </div>
                <div className="flex flex-wrap gap-2 pt-2">
                  <Button variant="primary" size="sm" isLoading>Loading</Button>
                  <Button variant="primary" size="sm" disabled>Disabled</Button>
                </div>
              </CardContent>
            </Card>

            {/* 2. Form Inputs Card */}
            <Card>
              <CardHeader>
                <CardTitle>Input Component</CardTitle>
                <CardDescription>
                  Validation errors, labels, và icons
                </CardDescription>
              </CardHeader>
              <CardContent className="space-y-3">
                <Input
                  label="Search Products"
                  placeholder="Nhập tên sản phẩm..."
                  value={inputValue}
                  onChange={(e) => {
                    setInputValue(e.target.value);
                    if (e.target.value.length > 0 && e.target.value.length < 3) {
                      setInputError('Từ khóa tối thiểu 3 ký tự');
                    } else {
                      setInputError('');
                    }
                  }}
                  leftIcon={<Search className="h-4 w-4" />}
                  error={inputError}
                  helperText={!inputError ? 'Hỗ trợ tìm kiếm theo tiếng Việt có dấu' : undefined}
                />
              </CardContent>
            </Card>

            {/* 3. Badges Card */}
            <Card>
              <CardHeader>
                <CardTitle>Badges & Statuses</CardTitle>
                <CardDescription>
                  Phân loại trạng thái đơn hàng & danh mục
                </CardDescription>
              </CardHeader>
              <CardContent className="space-y-3">
                <div className="flex flex-wrap gap-2">
                  <Badge variant="default">Default</Badge>
                  <Badge variant="secondary">In Stock</Badge>
                  <Badge variant="success">Paid</Badge>
                  <Badge variant="warning">Awaiting Payment</Badge>
                  <Badge variant="danger">Cancelled</Badge>
                  <Badge variant="outline">Electronics</Badge>
                </div>
              </CardContent>
            </Card>

            {/* 4. Product Card Simulation */}
            <Card hoverable className="overflow-hidden">
              <div className="relative aspect-video w-full bg-secondary-100 dark:bg-secondary-800 flex items-center justify-center">
                <Laptop className="h-12 w-12 text-secondary-400" />
                <Badge variant="success" className="absolute top-3 right-3">
                  -20% OFF
                </Badge>
              </div>
              <CardHeader>
                <div className="text-xs font-semibold uppercase tracking-wider text-primary-500">
                  Laptops & Gear
                </div>
                <CardTitle className="text-base line-clamp-1">
                  MacBook Pro 16" M3 Max
                </CardTitle>
                <CardDescription>
                  36GB Unified Memory, 1TB SSD Storage
                </CardDescription>
              </CardHeader>
              <CardFooter className="justify-between">
                <div>
                  <span className="text-lg font-bold text-text-primary">$2,499.00</span>
                </div>
                <Button variant="primary" size="sm" leftIcon={<ShoppingCart className="h-4 w-4" />}>
                  Thêm vào giỏ
                </Button>
              </CardFooter>
            </Card>

            {/* 5. Skeleton Loading State */}
            <Card>
              <CardHeader>
                <CardTitle>Skeleton Loaders</CardTitle>
                <CardDescription>
                  Mô phỏng placeholder khi tải dữ liệu API
                </CardDescription>
              </CardHeader>
              <CardContent className="space-y-3">
                <Skeleton className="h-4 w-3/4" />
                <Skeleton className="h-4 w-full" />
                <Skeleton className="h-4 w-5/6" />
                <div className="flex gap-2 pt-2">
                  <Skeleton className="h-8 w-20" />
                  <Skeleton className="h-8 w-24" />
                </div>
              </CardContent>
            </Card>

            {/* 6. System Architecture Status */}
            <Card>
              <CardHeader>
                <CardTitle>Hệ Thống Sẵn Sàng</CardTitle>
                <CardDescription>Kiểm tra tích hợp Giai đoạn 0</CardDescription>
              </CardHeader>
              <CardContent className="space-y-2 text-sm">
                <div className="flex items-center gap-2 text-emerald-600 dark:text-emerald-400">
                  <CheckCircle className="h-4 w-4" />
                  <span>Spring Boot 3.3 + Java 21 Context Verified</span>
                </div>
                <div className="flex items-center gap-2 text-emerald-600 dark:text-emerald-400">
                  <CheckCircle className="h-4 w-4" />
                  <span>Maven Wrapper (mvnw) Configured</span>
                </div>
                <div className="flex items-center gap-2 text-emerald-600 dark:text-emerald-400">
                  <CheckCircle className="h-4 w-4" />
                  <span>React 18 + Vite + Tailwind CSS Configured</span>
                </div>
                <div className="flex items-center gap-2 text-amber-600 dark:text-amber-400">
                  <AlertTriangle className="h-4 w-4" />
                  <span>Next: Giai đoạn 1 (Database & Flyway Migration)</span>
                </div>
              </CardContent>
            </Card>
          </div>
        </section>
      </main>

      {/* Demo Modal */}
      <Modal
        isOpen={isModalOpen}
        onClose={() => setIsModalOpen(false)}
        title="Modal Trợ Năng (WCAG AA)"
        description="Thành phần hộp thoại hỗ trợ phím Escape, backdrop click và khóa cuộn nền."
        footer={
          <>
            <Button variant="outline" size="sm" onClick={() => setIsModalOpen(false)}>
              Đóng
            </Button>
            <Button variant="primary" size="sm" onClick={() => setIsModalOpen(false)}>
              Xác Nhận
            </Button>
          </>
        }
      >
        <p className="text-sm text-text-secondary leading-relaxed">
          Modal này được render qua React Portal tại <code>document.body</code>. Nó đảm bảo tỷ lệ tương phản màu sắc và quản lý tiêu điểm bàn phím theo đúng tài liệu <code>docs/08-ui-design-system.md</code>.
        </p>
      </Modal>
    </div>
  );
}

