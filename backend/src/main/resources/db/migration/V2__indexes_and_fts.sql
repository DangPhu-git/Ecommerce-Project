-- ================================================================
-- V2__indexes_and_fts.sql
-- Ecommerce Platform - Advanced Indexing & PostgreSQL Full-Text Search
-- ================================================================

-- ----------------------------------------------------------------
-- 1. FULL-TEXT SEARCH (FTS) TRIGGER FOR PRODUCTS
-- ----------------------------------------------------------------
CREATE OR REPLACE FUNCTION products_search_vector_update()
RETURNS TRIGGER AS $$
BEGIN
    NEW.search_vector := to_tsvector('english', COALESCE(NEW.name, '') || ' ' || COALESCE(NEW.description, ''));
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

DROP TRIGGER IF EXISTS trg_products_search_vector_update ON products;
CREATE TRIGGER trg_products_search_vector_update
    BEFORE INSERT OR UPDATE OF name, description ON products
    FOR EACH ROW EXECUTE FUNCTION products_search_vector_update();

-- Backfill search_vector for any existing records
UPDATE products
SET search_vector = to_tsvector('english', COALESCE(name, '') || ' ' || COALESCE(description, ''))
WHERE search_vector IS NULL;

-- ----------------------------------------------------------------
-- 2. GIN INDEX FOR HIGH-PERFORMANCE FULL-TEXT SEARCH
-- ----------------------------------------------------------------
CREATE INDEX IF NOT EXISTS idx_products_search
    ON products USING GIN(search_vector);

-- ----------------------------------------------------------------
-- 3. COMPOSITE INDEX FOR CATALOG FILTERING & SORTING
-- ----------------------------------------------------------------
-- Tối ưu cho query: WHERE category_id = ? AND is_active = true ORDER BY created_at DESC
CREATE INDEX IF NOT EXISTS idx_products_category_active_created
    ON products(category_id, is_active, created_at DESC);

-- Partial index cho lọc theo khoảng giá đối với sản phẩm đang hoạt động
CREATE INDEX IF NOT EXISTS idx_products_price_active
    ON products(price) WHERE is_active = true;

-- ----------------------------------------------------------------
-- 4. PARTIAL INDEXES FOR ORDERS & PROCESSING WORKFLOWS
-- ----------------------------------------------------------------
-- Tối ưu hóa truy vấn đơn hàng đang chờ xử lý thanh toán / xác nhận (Admin & Worker)
CREATE INDEX IF NOT EXISTS idx_orders_status_pending
    ON orders(status) WHERE status = 'PENDING';

-- Tối ưu tra cứu nhanh Payment Intent từ Stripe Webhook
CREATE INDEX IF NOT EXISTS idx_orders_stripe_pi
    ON orders(stripe_payment_intent) WHERE stripe_payment_intent IS NOT NULL;

-- ----------------------------------------------------------------
-- 5. PARTIAL INDEXES FOR SECURITY & USERS
-- ----------------------------------------------------------------
-- Tìm kiếm user khi đăng nhập loại trừ tài khoản đã bị soft-deleted
CREATE INDEX IF NOT EXISTS idx_users_email_active
    ON users(email) WHERE deleted_at IS NULL;

-- Lọc danh sách phân quyền đối với user đang hoạt động
CREATE INDEX IF NOT EXISTS idx_users_role_active
    ON users(role) WHERE is_active = true;

-- Tra cứu token hash còn hiệu lực chưa bị revoke
CREATE INDEX IF NOT EXISTS idx_refresh_tokens_active
    ON refresh_tokens(token_hash) WHERE NOT revoked;

-- ----------------------------------------------------------------
-- 6. PARTIAL INDEX FOR WEBHOOK EVENTS INBOX POLLING
-- ----------------------------------------------------------------
-- Worker polling các sự kiện webhook đang chờ hoặc đang được xử lý
CREATE INDEX IF NOT EXISTS idx_webhook_events_queue
    ON webhook_events(status) WHERE status IN ('PENDING', 'PROCESSING');
