-- ================================================================
-- V1__initial_schema.sql
-- Ecommerce Platform - Initial Relational Schema
-- ================================================================

-- Enable UUID extension if available
CREATE EXTENSION IF NOT EXISTS "pgcrypto";

-- ----------------------------------------------------------------
-- 1. USERS
-- ----------------------------------------------------------------
CREATE TABLE users (
    id              UUID            PRIMARY KEY DEFAULT gen_random_uuid(),
    email           VARCHAR(255)    UNIQUE NOT NULL,
    password_hash   VARCHAR(255)    NOT NULL,
    first_name      VARCHAR(100)    NOT NULL,
    last_name       VARCHAR(100)    NOT NULL,
    role            VARCHAR(20)     NOT NULL DEFAULT 'USER'
                                    CHECK (role IN ('USER', 'ADMIN', 'SUPER_ADMIN')),
    is_active       BOOLEAN         NOT NULL DEFAULT true,
    created_at      TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
    deleted_at      TIMESTAMPTZ     NULL
);

CREATE INDEX idx_users_created_at ON users(created_at DESC);

-- ----------------------------------------------------------------
-- 2. REFRESH TOKENS
-- ----------------------------------------------------------------
CREATE TABLE refresh_tokens (
    id          UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id     UUID        NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    token_hash  VARCHAR(255) UNIQUE NOT NULL,
    expires_at  TIMESTAMPTZ NOT NULL,
    revoked     BOOLEAN     NOT NULL DEFAULT false,
    created_at  TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_refresh_tokens_user_id ON refresh_tokens(user_id);

-- ----------------------------------------------------------------
-- 3. CATEGORIES
-- ----------------------------------------------------------------
CREATE TABLE categories (
    id          UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
    name        VARCHAR(255) NOT NULL,
    slug        VARCHAR(255) UNIQUE NOT NULL,
    description TEXT,
    parent_id   UUID        REFERENCES categories(id) ON DELETE SET NULL,
    image_url   TEXT,
    sort_order  INTEGER     NOT NULL DEFAULT 0,
    created_at  TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_categories_parent_id ON categories(parent_id);
CREATE INDEX idx_categories_slug ON categories(slug);

-- ----------------------------------------------------------------
-- 4. PRODUCTS
-- ----------------------------------------------------------------
CREATE TABLE products (
    id              UUID            PRIMARY KEY DEFAULT gen_random_uuid(),
    name            VARCHAR(255)    NOT NULL,
    slug            VARCHAR(255)    UNIQUE NOT NULL,
    description     TEXT,
    price           DECIMAL(10,2)   NOT NULL CHECK (price >= 0),
    category_id     UUID            REFERENCES categories(id) ON DELETE SET NULL,
    inventory_count INTEGER         NOT NULL DEFAULT 0 CHECK (inventory_count >= 0),
    is_active       BOOLEAN         NOT NULL DEFAULT true,
    search_vector   TSVECTOR        NULL,
    created_at      TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMPTZ     NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_products_category ON products(category_id);
CREATE INDEX idx_products_price ON products(price);
CREATE INDEX idx_products_created_at ON products(created_at DESC);

-- ----------------------------------------------------------------
-- 5. PRODUCT VARIANTS
-- ----------------------------------------------------------------
CREATE TABLE product_variants (
    id              UUID            PRIMARY KEY DEFAULT gen_random_uuid(),
    product_id      UUID            NOT NULL REFERENCES products(id) ON DELETE CASCADE,
    name            VARCHAR(100)    NOT NULL,
    sku             VARCHAR(100)    UNIQUE NOT NULL,
    price_adjustment DECIMAL(10,2)  NOT NULL DEFAULT 0,
    inventory_count INTEGER         NOT NULL DEFAULT 0 CHECK (inventory_count >= 0),
    created_at      TIMESTAMPTZ     NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_product_variants_product_id ON product_variants(product_id);

-- ----------------------------------------------------------------
-- 6. PRODUCT IMAGES
-- ----------------------------------------------------------------
CREATE TABLE product_images (
    id          UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
    product_id  UUID        NOT NULL REFERENCES products(id) ON DELETE CASCADE,
    url         TEXT        NOT NULL,
    alt_text    VARCHAR(255),
    sort_order  INTEGER     NOT NULL DEFAULT 0,
    is_primary  BOOLEAN     NOT NULL DEFAULT false
);

CREATE INDEX idx_product_images_product_id ON product_images(product_id);

-- ----------------------------------------------------------------
-- 7. REVIEWS
-- ----------------------------------------------------------------
CREATE TABLE reviews (
    id          UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
    product_id  UUID        NOT NULL REFERENCES products(id) ON DELETE CASCADE,
    user_id     UUID        NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    rating      SMALLINT    NOT NULL CHECK (rating BETWEEN 1 AND 5),
    title       VARCHAR(255),
    body        TEXT,
    is_verified BOOLEAN     NOT NULL DEFAULT false,
    created_at  TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT uq_reviews_product_user UNIQUE (product_id, user_id)
);

CREATE INDEX idx_reviews_product_id ON reviews(product_id);
CREATE INDEX idx_reviews_user_id ON reviews(user_id);
CREATE INDEX idx_reviews_rating ON reviews(product_id, rating);

-- ----------------------------------------------------------------
-- 8. CARTS
-- ----------------------------------------------------------------
CREATE TABLE carts (
    id          UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id     UUID        REFERENCES users(id) ON DELETE CASCADE,
    session_id  VARCHAR(255) NULL,
    created_at  TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at  TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_carts_user_id ON carts(user_id);
CREATE INDEX idx_carts_session_id ON carts(session_id) WHERE session_id IS NOT NULL;

-- ----------------------------------------------------------------
-- 9. CART ITEMS
-- ----------------------------------------------------------------
CREATE TABLE cart_items (
    id          UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
    cart_id     UUID        NOT NULL REFERENCES carts(id) ON DELETE CASCADE,
    product_id  UUID        NOT NULL REFERENCES products(id) ON DELETE CASCADE,
    variant_id  UUID        REFERENCES product_variants(id) ON DELETE SET NULL,
    quantity    INTEGER     NOT NULL CHECK (quantity > 0),
    created_at  TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at  TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT uq_cart_items_cart_product_variant UNIQUE (cart_id, product_id, variant_id)
);

CREATE INDEX idx_cart_items_cart_id ON cart_items(cart_id);
CREATE INDEX idx_cart_items_product_id ON cart_items(product_id);

-- ----------------------------------------------------------------
-- 10. SHIPPING ADDRESSES
-- ----------------------------------------------------------------
CREATE TABLE shipping_addresses (
    id              UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id         UUID        NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    recipient_name  VARCHAR(100) NOT NULL,
    phone_number    VARCHAR(20)  NOT NULL,
    street_address  VARCHAR(255) NOT NULL,
    ward            VARCHAR(100),
    district        VARCHAR(100),
    city            VARCHAR(100) NOT NULL,
    postal_code     VARCHAR(20),
    country         VARCHAR(100) NOT NULL DEFAULT 'Vietnam',
    is_default      BOOLEAN     NOT NULL DEFAULT false,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_shipping_addresses_user_id ON shipping_addresses(user_id);

-- ----------------------------------------------------------------
-- 11. ORDERS
-- ----------------------------------------------------------------
CREATE TABLE orders (
    id                      UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id                 UUID        NOT NULL REFERENCES users(id),
    status                  VARCHAR(30) NOT NULL DEFAULT 'PENDING'
                                        CHECK (status IN (
                                            'PENDING', 'AWAITING_PAYMENT', 'PAID',
                                            'PROCESSING', 'SHIPPED', 'DELIVERED',
                                            'CANCELLED', 'REFUNDED'
                                        )),
    total_amount_minor      INTEGER     NOT NULL CHECK (total_amount_minor >= 0),
    currency                CHAR(3)     NOT NULL DEFAULT 'USD',
    payment_attempt         INTEGER     NOT NULL DEFAULT 1,
    stripe_payment_intent   VARCHAR(100),
    shipping_address        JSONB       NOT NULL,
    notes                   TEXT,
    created_at              TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at              TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_orders_user_id ON orders(user_id);
CREATE INDEX idx_orders_created_at ON orders(created_at DESC);

-- ----------------------------------------------------------------
-- 12. ORDER ITEMS
-- ----------------------------------------------------------------
CREATE TABLE order_items (
    id                  UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
    order_id            UUID        NOT NULL REFERENCES orders(id) ON DELETE CASCADE,
    product_id          UUID        REFERENCES products(id) ON DELETE SET NULL,
    variant_id          UUID        REFERENCES product_variants(id) ON DELETE SET NULL,
    quantity            INTEGER     NOT NULL CHECK (quantity > 0),
    unit_price_minor    INTEGER     NOT NULL CHECK (unit_price_minor >= 0),
    product_snapshot    JSONB       NOT NULL
);

CREATE INDEX idx_order_items_order_id ON order_items(order_id);
CREATE INDEX idx_order_items_product_id ON order_items(product_id);

-- ----------------------------------------------------------------
-- 13. PAYMENTS
-- ----------------------------------------------------------------
CREATE TABLE payments (
    id                  UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
    order_id            UUID        NOT NULL REFERENCES orders(id),
    stripe_payment_id   VARCHAR(100) NOT NULL,
    amount_minor        INTEGER     NOT NULL CHECK (amount_minor >= 0),
    currency            CHAR(3)     NOT NULL,
    status              VARCHAR(30) NOT NULL
                                    CHECK (status IN (
                                        'PENDING', 'PROCESSING', 'SUCCEEDED',
                                        'FAILED', 'CANCELLED', 'REFUNDED', 'PARTIALLY_REFUNDED'
                                    )),
    idempotency_key     VARCHAR(255) UNIQUE NOT NULL,
    failure_code        VARCHAR(100),
    failure_message     TEXT,
    refunded_amount     INTEGER     NOT NULL DEFAULT 0,
    created_at          TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at          TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_payments_order_id ON payments(order_id);
CREATE INDEX idx_payments_stripe_id ON payments(stripe_payment_id);
CREATE INDEX idx_payments_status ON payments(status);

-- ----------------------------------------------------------------
-- 14. LEDGER ENTRIES (Double-entry Ledger)
-- ----------------------------------------------------------------
CREATE TABLE ledger_entries (
    id              UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
    payment_id      UUID        REFERENCES payments(id),
    payout_id       VARCHAR(100),
    amount_minor    INTEGER     NOT NULL,
    currency        CHAR(3)     NOT NULL,
    type            VARCHAR(30) NOT NULL
                                CHECK (type IN (
                                    'CHARGE', 'REFUND', 'FEE', 'PAYOUT', 'ADJUSTMENT'
                                )),
    description     TEXT,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_ledger_entries_payment_id ON ledger_entries(payment_id);
CREATE INDEX idx_ledger_entries_payout_id ON ledger_entries(payout_id);
CREATE INDEX idx_ledger_entries_created_at ON ledger_entries(created_at DESC);

-- ----------------------------------------------------------------
-- 15. WEBHOOK EVENTS (Durable Inbox)
-- ----------------------------------------------------------------
CREATE TABLE webhook_events (
    id              UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
    event_id        VARCHAR(100) UNIQUE NOT NULL,
    event_type      VARCHAR(100) NOT NULL,
    payload         JSONB       NOT NULL,
    status          VARCHAR(20) NOT NULL DEFAULT 'PENDING'
                                CHECK (status IN ('PENDING', 'PROCESSING', 'COMPLETED', 'FAILED', 'DEAD_LETTER')),
    attempts        INTEGER     NOT NULL DEFAULT 0,
    last_error      TEXT,
    processed_at    TIMESTAMPTZ,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_webhook_events_event_type ON webhook_events(event_type);
CREATE INDEX idx_webhook_events_created_at ON webhook_events(created_at DESC);

-- ----------------------------------------------------------------
-- AUTO-UPDATE updated_at FUNCTION & TRIGGERS
-- ----------------------------------------------------------------
CREATE OR REPLACE FUNCTION update_updated_at_column()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = NOW();
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER update_users_updated_at
    BEFORE UPDATE ON users
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

CREATE TRIGGER update_products_updated_at
    BEFORE UPDATE ON products
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

CREATE TRIGGER update_carts_updated_at
    BEFORE UPDATE ON carts
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

CREATE TRIGGER update_cart_items_updated_at
    BEFORE UPDATE ON cart_items
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

CREATE TRIGGER update_shipping_addresses_updated_at
    BEFORE UPDATE ON shipping_addresses
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

CREATE TRIGGER update_orders_updated_at
    BEFORE UPDATE ON orders
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

CREATE TRIGGER update_payments_updated_at
    BEFORE UPDATE ON payments
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();
