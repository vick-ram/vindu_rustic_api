CREATE EXTENSION IF NOT EXISTS "uuid-ossp";
CREATE EXTENSION IF NOT EXISTS pgcrypto;
CREATE EXTENSION IF NOT EXISTS citext;

CREATE TABLE users
(
    id             BIGSERIAL PRIMARY KEY,
    uuid           UUID UNIQUE DEFAULT gen_random_uuid(),
    email          CITEXT UNIQUE NOT NULL,
    password       TEXT          NOT NULL,
    first_name     VARCHAR(120),
    last_name      VARCHAR(120),
    phone          VARCHAR(30),
    avatar         TEXT,
    email_verified BOOLEAN     DEFAULT FALSE,
    phone_verified BOOLEAN     DEFAULT FALSE,
    status         VARCHAR(30) DEFAULT 'active',
    last_login_at  TIMESTAMPTZ,
    created_at     TIMESTAMPTZ DEFAULT NOW(),
    updated_at     TIMESTAMPTZ DEFAULT NOW(),
    deleted_at     timestamptz
);

CREATE TABLE roles
(
    id   BIGSERIAL PRIMARY KEY,
    name VARCHAR(100) UNIQUE NOT NULL
);

CREATE TABLE permissions
(
    id   BIGSERIAL PRIMARY KEY,
    name VARCHAR(100) UNIQUE NOT NULL
);

CREATE TABLE user_roles
(
    user_id BIGINT REFERENCES users (id) ON DELETE CASCADE,
    role_id BIGINT REFERENCES roles (id) ON DELETE CASCADE,

    PRIMARY KEY (user_id, role_id)
);


CREATE TABLE addresses
(
    id             BIGSERIAL PRIMARY KEY,
    user_id        BIGINT REFERENCES users (id) ON DELETE CASCADE,
    label          VARCHAR(100),
    recipient_name VARCHAR(255),
    phone_number   VARCHAR(30),
    country_code   VARCHAR(10),
    country        VARCHAR(120),
    city           VARCHAR(120),
    county         VARCHAR(100),
    postal_code    VARCHAR(30),
    address_line1  TEXT NOT NULL,
    address_lin2   TEXT,
    latitude       NUMERIC(10, 7),
    longitude      NUMERIC(10, 7),
    is_default     BOOLEAN                  DEFAULT FALSE,
    created_at     TIMESTAMP WITH TIME ZONE DEFAULT NOW()
);

CREATE TABLE categories
(
    id          BIGSERIAL PRIMARY KEY,
    parent_id   BIGINT REFERENCES categories (id),
    name        VARCHAR(255)        NOT NULL,
    slug        VARCHAR(255) UNIQUE NOT NULL,
    description TEXT,
    image_url   TEXT,
    is_active   BOOLEAN                  DEFAULT FALSE,
    sort_order  INT                      DEFAULT 0,
    created_at  TIMESTAMP WITH TIME ZONE DEFAULT NOW()
);

CREATE TABLE products
(
    id                BIGSERIAL PRIMARY KEY,
    category_id       BIGINT REFERENCES categories (id),
    title             VARCHAR(255)        NOT NULL,
    slug              VARCHAR(255) UNIQUE NOT NULL,
    short_description TEXT,
    description       TEXT,
    status            VARCHAR(50)              DEFAULT 'DRAFT',
    product_type      VARCHAR(50)              DEFAULT 'STANDARD',
    brand             VARCHAR(255),
    is_customizable   BOOLEAN                  DEFAULT FALSE,
    is_featured       BOOLEAN                  DEFAULT FALSE,
    seo_title         VARCHAR(255),
    seo_description   TEXT,
    created_at        TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    updated_at        TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    deleted_at        TIMESTAMPTZ
);

CREATE TABLE product_variants
(
    id                BIGSERIAL PRIMARY KEY,
    product_id        BIGINT REFERENCES products (id) ON DELETE CASCADE,
    sku               VARCHAR(100) UNIQUE NOT NULL,
    title             VARCHAR(255),
    price             NUMERIC(12, 2)      NOT NULL,
    compare_at_price  NUMERIC(12, 2),
    cost_price        NUMERIC(12, 2),
    currency          VARCHAR(10) DEFAULT 'KES',
    quantity_in_stock INT         DEFAULT 0,
    reserved_quantity INT         DEFAULT 0,
    weight_grams      INT,
    dimensions        JSONB,
    attributes        JSONB       DEFAULT '{}'::jsonb,
    is_active         BOOLEAN     DEFAULT TRUE,
    created_at        TIMESTAMPTZ DEFAULT NOW()
);


CREATE TABLE product_media
(
    id         BIGSERIAL PRIMARY KEY,
    product_id BIGINT REFERENCES products (id) ON DELETE CASCADE,
    variant_id BIGINT REFERENCES product_variants (id),
    media_type VARCHAR(50),
    media_url  TEXT NOT NULL,
    alt_text   TEXT,
    sort_order INT         DEFAULT 0,
    created_at TIMESTAMPTZ DEFAULT NOW()
);

CREATE TABLE custom_product_requests
(
    id               BIGSERIAL PRIMARY KEY,
    user_id          BIGINT REFERENCES users (id),
    title            VARCHAR(255),
    description      TEXT,
    dimensions       JSONB,
    estimated_budget NUMERIC(12, 2),
    status           VARCHAR(50) DEFAULT 'PENDING',
    created_at       TIMESTAMPTZ DEFAULT NOW()
);

CREATE TABLE custom_product_attachments
(
    id         BIGSERIAL PRIMARY KEY,
    request_id BIGINT REFERENCES custom_product_requests (id),
    file_url   TEXT NOT NULL,
    created_at TIMESTAMPTZ DEFAULT NOW()
);

CREATE TABLE warehouses
(
    id         BIGSERIAL PRIMARY KEY,
    name       VARCHAR(255),
    address    TEXT,
    created_at TIMESTAMPTZ DEFAULT NOW()
);

CREATE TABLE inventory
(
    id                 BIGSERIAL PRIMARY KEY,
    variant_id         BIGINT REFERENCES product_variants (id),
    warehouse_id       BIGINT REFERENCES warehouses (id),
    available_quantity INT         DEFAULT 0,
    reserved_quantity  INT         DEFAULT 0,
    damaged_quantity   INT         DEFAULT 0,
    updated_at         TIMESTAMPTZ DEFAULT NOW()
);

CREATE TABLE inventory_movements
(
    id             BIGSERIAL PRIMARY KEY,
    variant_id     BIGINT REFERENCES product_variants (id),
    movement_type  VARCHAR(50),
    quantity       INT NOT NULL,
    reference_type VARCHAR(100),
    reference_id   BIGINT,
    created_at     TIMESTAMPTZ DEFAULT NOW()
);

CREATE TABLE shopping_carts
(
    id            BIGSERIAL PRIMARY KEY,
    user_id       BIGINT REFERENCES users (id),
    session_token TEXT,
    created_at    TIMESTAMPTZ DEFAULT NOW(),
    updated_at    TIMESTAMPTZ DEFAULT NOW()
);


CREATE TABLE cart_items
(
    id         BIGSERIAL PRIMARY KEY,
    cart_id    BIGINT REFERENCES shopping_carts (id) ON DELETE CASCADE,
    variant_id BIGINT REFERENCES product_variants (id),
    quantity   INT NOT NULL
);

CREATE TABLE orders
(
    id                 BIGSERIAL PRIMARY KEY,
    order_number       UUID        DEFAULT gen_random_uuid(),
    user_id            BIGINT REFERENCES users (id),
    address_id         BIGINT REFERENCES addresses (id),
    status             VARCHAR(50) DEFAULT 'PENDING',
    payment_status     VARCHAR(50) DEFAULT 'PENDING',
    fulfillment_status VARCHAR(50) DEFAULT 'UNFULFILLED',
    currency           VARCHAR(10) DEFAULT 'KES',
    subtotal           NUMERIC(12, 2),
    shipping_cost      NUMERIC(12, 2),
    tax_amount         NUMERIC(12, 2),
    discount_amount    NUMERIC(12, 2),
    total_amount       NUMERIC(12, 2),
    notes              TEXT,
    placed_at          TIMESTAMPTZ DEFAULT NOW(),
    updated_at         TIMESTAMPTZ DEFAULT NOW()
);

CREATE TABLE order_items
(
    id                     BIGSERIAL PRIMARY KEY,
    order_id               BIGINT REFERENCES orders (id) ON DELETE CASCADE,
    product_id             BIGINT REFERENCES products (id),
    variant_id             BIGINT REFERENCES product_variants (id),
    quantity               INT NOT NULL,
    unit_price             NUMERIC(12, 2),
    total_price            NUMERIC(12, 2),
    customization_snapshot JSONB,
    product_snapshot       JSONB
);

CREATE TABLE payments
(
    id                    BIGSERIAL PRIMARY KEY,
    order_id              BIGINT REFERENCES orders (id),
    provider              VARCHAR(100),
    transaction_reference VARCHAR(255),
    amount                NUMERIC(12, 2),
    currency              VARCHAR(10),
    status                VARCHAR(50),
    paid_at               TIMESTAMPTZ,
    created_at            TIMESTAMPTZ DEFAULT NOW()
);

CREATE TABLE refunds
(
    id         BIGSERIAL PRIMARY KEY,
    payment_id BIGINT REFERENCES payments (id),
    amount     NUMERIC(12, 2),
    reason     TEXT,
    status     VARCHAR(50),
    created_at TIMESTAMPTZ DEFAULT NOW()
);

CREATE TABLE shipments
(
    id              BIGSERIAL PRIMARY KEY,
    order_id        BIGINT REFERENCES orders (id),
    courier         VARCHAR(255),
    tracking_number VARCHAR(255),
    status          VARCHAR(50),
    shipped_at      TIMESTAMPTZ,
    delivered_at    TIMESTAMPTZ
);

CREATE TABLE production_jobs
(
    id            BIGSERIAL PRIMARY KEY,
    order_item_id BIGINT REFERENCES order_items (id),
    status        VARCHAR(50),
    started_at    TIMESTAMPTZ,
    completed_at  TIMESTAMPTZ,
    notes         TEXT
);

CREATE TABLE production_stages
(
    id           BIGSERIAL PRIMARY KEY,
    job_id       BIGINT REFERENCES production_jobs (id) ON DELETE CASCADE,
    stage_name   VARCHAR(255),
    status       VARCHAR(50),
    started_at   TIMESTAMPTZ,
    completed_at TIMESTAMPTZ
);

CREATE TABLE product_reviews
(
    id                   BIGSERIAL PRIMARY KEY,
    product_id           BIGINT REFERENCES products (id),
    user_id              BIGINT REFERENCES users (id),
    order_item_id        BIGINT REFERENCES order_items (id),
    rating               INT CHECK (rating BETWEEN 1 AND 5),
    title                VARCHAR(255),
    review               TEXT,
    is_verified_purchase BOOLEAN     DEFAULT FALSE,
    created_at           TIMESTAMPTZ DEFAULT NOW()
);

CREATE TABLE support_tickets
(
    ticket_id  BIGSERIAL PRIMARY KEY,
    user_id    BIGINT REFERENCES users (id),
    order_id   BIGINT REFERENCES orders (id),
    subject    VARCHAR(255),
    status     VARCHAR(50),
    priority   VARCHAR(50),
    created_at TIMESTAMPTZ DEFAULT NOW()
);

CREATE TABLE coupons
(
    coupon_id   BIGSERIAL PRIMARY KEY,
    code        VARCHAR(100) UNIQUE,
    type        VARCHAR(50),
    value       NUMERIC(12, 2),
    usage_limit INT,
    starts_at   TIMESTAMPTZ,
    ends_at     TIMESTAMPTZ
);

CREATE TABLE notifications
(
    notification_id BIGSERIAL PRIMARY KEY,
    user_id         BIGINT REFERENCES users (id),
    type            VARCHAR(100),
    title           VARCHAR(255),
    body            TEXT,
    is_read         BOOLEAN     DEFAULT FALSE,
    created_at      TIMESTAMPTZ DEFAULT NOW()
);

CREATE TABLE user_events
(
    event_id    BIGSERIAL PRIMARY KEY,
    user_id     BIGINT REFERENCES users (id),
    event_type  VARCHAR(100),
    entity_type VARCHAR(100),
    entity_id   BIGINT,
    metadata    JSONB,
    created_at  TIMESTAMPTZ DEFAULT NOW()
);

CREATE TABLE audit_logs
(
    id          BIGSERIAL PRIMARY KEY,
    actor_id    BIGINT,
    actor_type  VARCHAR(100),
    entity_type VARCHAR(100),
    entity_id   BIGINT,
    action      VARCHAR(100),
    old_data    JSONB,
    new_data    JSONB,
    created_at  TIMESTAMPTZ DEFAULT NOW()
);

CREATE TABLE outbox_events
(
    id             BIGSERIAL PRIMARY KEY,
    aggregate_type VARCHAR(100),
    aggregate_id   BIGINT,
    event_type     VARCHAR(100),
    payload        JSONB,
    processed      BOOLEAN     DEFAULT FALSE,
    created_at     TIMESTAMPTZ DEFAULT NOW()
);

CREATE INDEX idx_orders_user_id ON orders (user_id);

CREATE INDEX idx_products_category_id ON products (category_id);

CREATE INDEX idx_order_items_order_id ON order_items (order_id);

CREATE INDEX idx_inventory_variant_id ON inventory (variant_id);

CREATE INDEX idx_payments_order_id ON payments (order_id);

CREATE INDEX idx_events_processed ON outbox_events (processed);