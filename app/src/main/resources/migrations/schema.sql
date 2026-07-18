CREATE TABLE users
(
    id             VARCHAR(26) PRIMARY KEY DEFAULT generate_ulid(),
    first_name     VARCHAR(120),
    last_name      VARCHAR(120),
    email          CITEXT NOT NULL,
    password       TEXT          NOT NULL,
    phone          VARCHAR(30),
    avatar         TEXT,
    email_verified BOOLEAN                 DEFAULT FALSE,
    phone_verified BOOLEAN                 DEFAULT FALSE,
    status         VARCHAR(30)             DEFAULT 'active',
    enable_2fa     BOOLEAN                 DEFAULT FALSE,
    last_login_at  TIMESTAMPTZ,
    created_at     TIMESTAMPTZ             DEFAULT NOW(),
    updated_at     TIMESTAMPTZ             DEFAULT NOW(),
    deleted_at     timestamptz,

    CONSTRAINT chk_user_status CHECK ( status IN ('active', 'suspended', 'deactivated') )
);

CREATE TABLE sessions
(
    id            VARCHAR(26) PRIMARY KEY DEFAULT generate_ulid(),
    user_id       VARCHAR(26) NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    refresh_token TEXT        NOT NULL,
    ip_address    INET,
    user_agent    TEXT,
    expires_at    TIMESTAMPTZ NOT NULL,
    created_at    TIMESTAMPTZ             DEFAULT NOW()
);

CREATE TABLE roles
(
    id          VARCHAR(26) PRIMARY KEY DEFAULT generate_ulid(),
    name        VARCHAR(100) UNIQUE NOT NULL,
    description TEXT
);

CREATE TABLE user_roles
(
    user_id VARCHAR(26) REFERENCES users (id) ON DELETE CASCADE,
    role_id VARCHAR(26) REFERENCES roles (id) ON DELETE CASCADE,

    PRIMARY KEY (user_id, role_id)
);


CREATE TABLE addresses
(
    id             VARCHAR(26) PRIMARY KEY DEFAULT generate_ulid(),
    user_id        VARCHAR(26) REFERENCES users (id) ON DELETE CASCADE,
    label          VARCHAR(100),
    recipient_name VARCHAR(255),
    phone_number   VARCHAR(30),
    country_code   VARCHAR(10)             DEFAULT 'ke',
    country        VARCHAR(120),
    city           VARCHAR(120),
    county         VARCHAR(100),
    postal_code    VARCHAR(30),
    address_line1  TEXT NOT NULL,
    address_line2   TEXT,
    latitude       NUMERIC(10, 7),
    longitude      NUMERIC(10, 7),
    is_default     BOOLEAN                 DEFAULT FALSE,
    created_at     TIMESTAMPTZ             DEFAULT NOW()
);

CREATE TABLE warehouses
(
    id         VARCHAR(26) PRIMARY KEY DEFAULT generate_ulid(),
    name       VARCHAR(255),
    address    VARCHAR(26) REFERENCES addresses (id) ON DELETE CASCADE,
    created_at TIMESTAMPTZ             DEFAULT NOW()
);

CREATE TABLE categories
(
    id          VARCHAR(26) PRIMARY KEY DEFAULT generate_ulid(),
    parent_id   VARCHAR(26) REFERENCES categories (id),
    name        VARCHAR(255)        NOT NULL,
    slug        VARCHAR(255) UNIQUE NOT NULL,
    description TEXT,
    image_url   TEXT,
    is_active   BOOLEAN                 DEFAULT FALSE,
    sort_order  INT                     DEFAULT 0,
    created_at  TIMESTAMPTZ             DEFAULT NOW()
);

CREATE TABLE products
(
    id                VARCHAR(26) PRIMARY KEY DEFAULT generate_ulid(),
    category_id       VARCHAR(26) REFERENCES categories (id),
    title             VARCHAR(255)        NOT NULL,
    slug              VARCHAR(255) UNIQUE NOT NULL,
    short_description TEXT,
    description       TEXT,
    status            VARCHAR(50)             DEFAULT 'draft',
    product_type      VARCHAR(50)             DEFAULT 'standard',
    brand             VARCHAR(255),
    is_customizable   BOOLEAN                 DEFAULT FALSE,
    is_featured       BOOLEAN                 DEFAULT FALSE,
    seo_title         VARCHAR(255),
    seo_description   TEXT,
    created_at        TIMESTAMPTZ             DEFAULT NOW(),
    updated_at        TIMESTAMPTZ             DEFAULT NOW(),
    deleted_at        TIMESTAMPTZ,

    CONSTRAINT chk_product_status CHECK ( status IN ('draft', 'published', 'archived') ),
    CONSTRAINT chk_product_type CHECK ( product_type IN ('standard', 'digital', 'customizable', 'bundled') )
);

CREATE TABLE product_variants
(
    id               VARCHAR(26) PRIMARY KEY DEFAULT generate_ulid(),
    product_id       VARCHAR(26) REFERENCES products (id) ON DELETE CASCADE,
    sku              VARCHAR(100) UNIQUE NOT NULL,
    title            VARCHAR(255),
    price            NUMERIC(12, 2)      NOT NULL,
    compare_at_price NUMERIC(12, 2),
    cost_price       NUMERIC(12, 2),
    weight_grams     INT,
    dimensions       JSONB,
    attributes       JSONB                   DEFAULT '{}'::jsonb,
    is_active        BOOLEAN                 DEFAULT TRUE,
    created_at       TIMESTAMPTZ             DEFAULT NOW()
);

CREATE TABLE product_media
(
    id         VARCHAR(26) PRIMARY KEY DEFAULT generate_ulid(),
    product_id VARCHAR(26) REFERENCES products (id) ON DELETE CASCADE,
    variant_id VARCHAR(26) REFERENCES product_variants (id),
    media_type VARCHAR(50),
    media_url  TEXT NOT NULL,
    alt_text   TEXT,
    sort_order INT                     DEFAULT 0,
    created_at TIMESTAMPTZ             DEFAULT NOW()
);

CREATE TABLE tags
(

    id         VARCHAR(26) PRIMARY KEY DEFAULT generate_ulid(),
    name       VARCHAR(100) UNIQUE NOT NULL,
    slug       VARCHAR(120) UNIQUE NOT NULL,
    created_at TIMESTAMPTZ             DEFAULT NOW()
);

CREATE TABLE product_tags
(
    product_id VARCHAR(26) NOT NULL REFERENCES products (id) ON DELETE CASCADE,
    tag_id     VARCHAR(26) NOT NULL REFERENCES tags (id) ON DELETE CASCADE,
    created_at TIMESTAMPTZ DEFAULT NOW(),
    PRIMARY KEY (product_id, tag_id)
);

CREATE TABLE custom_product_requests
(
    id                   VARCHAR(26) PRIMARY KEY DEFAULT generate_ulid(),
    user_id              VARCHAR(26) REFERENCES users (id),
    title                VARCHAR(255),
    description          TEXT,
    specifications       JSONB,
    estimated_budget_min NUMERIC(12, 2),
    estimated_budget_max NUMERIC(12, 2),
    status               VARCHAR(50)             DEFAULT 'pending',
    notes                TEXT,
    created_at           TIMESTAMPTZ             DEFAULT NOW(),

    CONSTRAINT chk_custom_request_status
        CHECK (status IN ('pending', 'under_review', 'quoted', 'accepted', 'rejected', 'cancelled'))
);

CREATE TABLE custom_product_attachments
(
    id         VARCHAR(26) PRIMARY KEY DEFAULT generate_ulid(),
    request_id VARCHAR(26) REFERENCES custom_product_requests (id),
    file_url   TEXT NOT NULL,
    file_type  VARCHAR(50),
    file_size  BIGINT,
    created_at TIMESTAMPTZ             DEFAULT NOW()
);

CREATE TABLE custom_product_quotes
(
    id                       VARCHAR(26) PRIMARY KEY DEFAULT generate_ulid(),
    request_id               VARCHAR(26)    NOT NULL REFERENCES custom_product_requests (id) ON DELETE CASCADE,
    quoted_price             NUMERIC(12, 2) NOT NULL,
    currency                 VARCHAR(10)             DEFAULT 'kes',
    production_timeline_days INT,
    description              TEXT,
    valid_until              TIMESTAMPTZ,
    status                   VARCHAR(50)             DEFAULT 'sent',
    created_by               VARCHAR(26)    NOT NULL REFERENCES users (id),
    created_at               TIMESTAMPTZ             DEFAULT NOW(),
    updated_at               TIMESTAMPTZ             DEFAULT NOW(),

    CONSTRAINT chk_custom_quote_status
        CHECK (status IN ('sent', 'accepted', 'expired', 'rejected')),
    CONSTRAINT chk_custom_quote_currency CHECK ( currency ~ '^[a-z]{3}$' )
);

CREATE TABLE shopping_carts
(
    id          VARCHAR(26) PRIMARY KEY DEFAULT generate_ulid(),
    user_id     VARCHAR(26) REFERENCES users (id),
    guest_token UUID                    DEFAULT gen_random_uuid(),
    created_at  TIMESTAMPTZ             DEFAULT NOW(),
    updated_at  TIMESTAMPTZ             DEFAULT NOW(),

    CONSTRAINT cart_has_identity CHECK (user_id IS NOT NULL OR guest_token IS NOT NULL)
);


CREATE TABLE cart_items
(
    id                    VARCHAR(26) PRIMARY KEY DEFAULT generate_ulid(),
    cart_id               VARCHAR(26) REFERENCES shopping_carts (id) ON DELETE CASCADE,
    variant_id            VARCHAR(26) REFERENCES product_variants (id),
    quantity              INT NOT NULL,
    customization_details JSONB                   DEFAULT '{}'
);

CREATE TABLE wishlists
(
    id         VARCHAR(26) PRIMARY KEY DEFAULT generate_ulid(),
    user_id    VARCHAR(26) NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    name       VARCHAR(255) DEFAULT 'Default',
    is_public  BOOLEAN      DEFAULT FALSE,
    created_at TIMESTAMPTZ  DEFAULT NOW(),

    UNIQUE (user_id, name)

);

CREATE TABLE wishlist_items
(
    id VARCHAR(26) PRIMARY KEY DEFAULT generate_ulid(),
    wishlist_id      VARCHAR(26) NOT NULL REFERENCES wishlists (id) ON DELETE CASCADE,
    product_id       VARCHAR(26) NOT NULL REFERENCES products (id) ON DELETE CASCADE,
    variant_id       VARCHAR(26) REFERENCES product_variants (id) ON DELETE SET NULL,
    notes            TEXT,
    created_at       TIMESTAMPTZ             DEFAULT NOW(),

    UNIQUE (wishlist_id, product_id)
);

CREATE TABLE orders
(
    id                  VARCHAR(26) PRIMARY KEY DEFAULT generate_ulid(),
    order_number        UUID                    DEFAULT gen_random_uuid(),
    user_id             VARCHAR(26) REFERENCES users (id),
    shipping_address_id VARCHAR(26) REFERENCES addresses (id),
    billing_address_id  VARCHAR(26) REFERENCES addresses (id),
    status              VARCHAR(50)             DEFAULT 'pending',
    payment_status      VARCHAR(50)             DEFAULT 'pending',
    fulfillment_status  VARCHAR(50)             DEFAULT 'unfulfilled',
    currency            VARCHAR(10)             DEFAULT 'kes',
    subtotal            NUMERIC(12, 2),
    shipping_cost       NUMERIC(12, 2),
    tax_amount          NUMERIC(12, 2),
    discount_amount     NUMERIC(12, 2),
    total_amount        NUMERIC(12, 2),
    coupon_code         VARCHAR(100),
    notes               TEXT,
    placed_at           TIMESTAMPTZ             DEFAULT NOW(),
    updated_at          TIMESTAMPTZ             DEFAULT NOW(),

    CONSTRAINT chk_orders_total CHECK ( total_amount >= 0 ),
 CONSTRAINT chk_order_status
        CHECK (status IN ('pending', 'confirmed', 'processing', 'completed', 'cancelled', 'refunded')),
     CONSTRAINT chk_order_payment_status
        CHECK (payment_status IN ('pending', 'authorized', 'paid', 'partially_refunded', 'refunded', 'failed')),
     CONSTRAINT chk_order_fulfillment_status
        CHECK (fulfillment_status IN ('unfulfilled', 'partially_fulfilled', 'fulfilled', 'restocked')),
    CONSTRAINT chk_order_currency CHECK ( currency ~ '^[a-z]{3}$' )
);

CREATE TABLE order_items
(
    id                     VARCHAR(26) PRIMARY KEY DEFAULT generate_ulid(),
    order_id               VARCHAR(26) REFERENCES orders (id) ON DELETE CASCADE,
    product_id             VARCHAR(26) REFERENCES products (id),
    variant_id             VARCHAR(26) REFERENCES product_variants (id),
    warehouse_id           VARCHAR(26) REFERENCES warehouses (id),
    quantity               INT NOT NULL,
    unit_price             NUMERIC(12, 2),
    total_price            NUMERIC(12, 2),
    customization_snapshot JSONB,
    product_snapshot       JSONB,

    CONSTRAINT chk_order_items_price CHECK ( unit_price >= 0 )
);

CREATE TABLE product_reviews
(
    id                   VARCHAR(26) PRIMARY KEY DEFAULT generate_ulid(),
    product_id           VARCHAR(26) REFERENCES products (id),
    user_id              VARCHAR(26) REFERENCES users (id),
    order_item_id        VARCHAR(26) REFERENCES order_items (id),
    rating               INT CHECK (rating BETWEEN 1 AND 5),
    title                VARCHAR(255),
    review               TEXT,
    is_verified_purchase BOOLEAN                 DEFAULT FALSE,
    created_at           TIMESTAMPTZ             DEFAULT NOW()
);

CREATE TABLE order_status_history
(
    id VARCHAR(26) PRIMARY KEY DEFAULT generate_ulid(),
    order_id   VARCHAR(26) NOT NULL REFERENCES orders (id) ON DELETE CASCADE,
    old_status VARCHAR(50),
    new_status VARCHAR(50) NOT NULL,
    changed_by VARCHAR(26) REFERENCES users (id),
    comment    TEXT,
    created_at TIMESTAMPTZ             DEFAULT NOW(),

    CONSTRAINT chk_history_status CHECK ( new_status IN ('pending', 'confirmed', 'processing', 'completed', 'cancelled', 'refunded') )
);

CREATE TABLE inventory
(
    id                  VARCHAR(26) PRIMARY KEY DEFAULT generate_ulid(),
    variant_id          VARCHAR(26) REFERENCES product_variants (id),
    warehouse_id        VARCHAR(26) REFERENCES warehouses (id),
    available_quantity  INT                     DEFAULT 0,
    reserved_quantity   INT                     DEFAULT 0,
    damaged_quantity    INT                     DEFAULT 0,
    low_stock_threshold BIGINT,
    updated_at          TIMESTAMPTZ             DEFAULT NOW(),

    CONSTRAINT chk_inventory_quantities CHECK ( available_quantity >= 0 AND reserved_quantity >= 0 AND damaged_quantity >= 0)
);

CREATE TABLE inventory_movements
(
    id             VARCHAR(26) PRIMARY KEY DEFAULT generate_ulid(),
    variant_id     VARCHAR(26) REFERENCES product_variants (id),
    movement_type  VARCHAR(50),
    quantity       INT NOT NULL,
    reference_type VARCHAR(100),
    reference_id   VARCHAR(26),
    notes          TEXT,
    created_at     TIMESTAMPTZ             DEFAULT NOW(),


    CONSTRAINT chk_inventory_movement_type
        CHECK (movement_type IN ('inbound', 'outbound', 'adjustment', 'reservation_hold', 'reservation_release'))
);

CREATE TABLE inventory_reservations
(
    id           VARCHAR(26) PRIMARY KEY DEFAULT generate_ulid(),
    variant_id   VARCHAR(26) NOT NULL REFERENCES product_variants (id),
    warehouse_id VARCHAR(26) NOT NULL REFERENCES warehouses (id),
    cart_id      VARCHAR(26) REFERENCES shopping_carts (id),
    order_id     VARCHAR(26) REFERENCES orders (id),
    quantity     INT         NOT NULL,
    status       VARCHAR(50)             DEFAULT 'active',
    expires_at   TIMESTAMPTZ NOT NULL,
    created_at   TIMESTAMPTZ             DEFAULT NOW(),

    CONSTRAINT chk_inventory_reservation_status
        CHECK (status IN ('active', 'completed', 'expired', 'cancelled'))
);

CREATE TABLE payments
(
    id             VARCHAR(26) PRIMARY KEY DEFAULT generate_ulid(),
    order_id       VARCHAR(26) REFERENCES orders (id),
    provider       VARCHAR(100),
    amount         NUMERIC(12, 2),
    currency       VARCHAR(10),
    status         VARCHAR(50)             DEFAULT 'initiated',
    payment_method VARCHAR(100),
    created_at     TIMESTAMPTZ             DEFAULT NOW(),
    updated_at     TIMESTAMPTZ             DEFAULT NOW(),

    CONSTRAINT chk_payments_amount CHECK ( amount > 0 ),
    CONSTRAINT chk_payment_status
        CHECK (status IN ('initiated', 'authorized', 'successful', 'failed', 'voided')),
    CONSTRAINT chk_payments_currency CHECK ( currency ~ '^[a-z]{3}$' )
);

CREATE TABLE payment_transactions
(
    id                      VARCHAR(26) PRIMARY KEY DEFAULT generate_ulid(),
    payment_id              VARCHAR(26)    NOT NULL REFERENCES payments (id),
    provider_transaction_id VARCHAR(255),
    transaction_type        VARCHAR(50)    NOT NULL,
    amount                  NUMERIC(12, 2) NOT NULL,
    currency                VARCHAR(10)             DEFAULT 'kes',
    status                  VARCHAR(50)    NOT NULL,
    provider_response       JSONB,
    error_message           TEXT,
    created_at              TIMESTAMPTZ             DEFAULT NOW(),

    CONSTRAINT chk_transaction_type
        CHECK (transaction_type IN ('charge', 'authorize', 'capture', 'refund', 'void')),
     CONSTRAINT chk_transaction_status
        CHECK (status IN ('success', 'failure', 'pending')),
    CONSTRAINT chk_payment_transactions_currency CHECK ( currency ~ '^[a-z]{3}$' )
);

CREATE TABLE refunds
(
    id      VARCHAR(26) PRIMARY KEY DEFAULT generate_ulid(),
    payment_id     VARCHAR(26)    NOT NULL REFERENCES payments (id),
    transaction_id VARCHAR(26) REFERENCES payment_transactions (id),
    amount         NUMERIC(12, 2) NOT NULL,
    reason         TEXT,
    status         VARCHAR(50)    NOT NULL DEFAULT 'requested',
    requested_by   VARCHAR(26) REFERENCES users (id),
    processed_by   VARCHAR(26) REFERENCES users (id),
    created_at     TIMESTAMPTZ             DEFAULT NOW(),
    processed_at   TIMESTAMPTZ,

    CONSTRAINT chk_refund_status
        CHECK (status IN ('requested', 'approved', 'processed', 'rejected', 'failed'))
);

CREATE TABLE shipments
(
    id           VARCHAR(26) PRIMARY KEY DEFAULT generate_ulid(),
    order_id              VARCHAR(26) NOT NULL REFERENCES orders (id),
    warehouse_id          VARCHAR(26) REFERENCES warehouses (id),
    courier               VARCHAR(255),
    service_level         VARCHAR(100),
    tracking_number       VARCHAR(255),
    tracking_url          TEXT,
    shipping_label_url    TEXT,
    cost                  NUMERIC(12, 2),
    estimated_delivery_at TIMESTAMPTZ,
    created_at            TIMESTAMPTZ             DEFAULT NOW(),
    updated_at            TIMESTAMPTZ             DEFAULT NOW(),

    CONSTRAINT chk_shipment_service_level
        CHECK (service_level IN ('standard', 'express', 'next_day'))
);


CREATE TABLE shipment_events
(
    id    VARCHAR(26) PRIMARY KEY DEFAULT generate_ulid(),
    shipment_id VARCHAR(26) NOT NULL REFERENCES shipments (shipment_id) ON DELETE CASCADE,
    event_type  VARCHAR(50) NOT NULL,
    status      VARCHAR(100),
    location    VARCHAR(255),
    description TEXT,
    occurred_at TIMESTAMPTZ NOT NULL,
    created_at  TIMESTAMPTZ             DEFAULT NOW(),

    CONSTRAINT chk_shipment_event_type
        CHECK (event_type IN ('picked_up', 'in_transit', 'out_for_delivery', 'delivered', 'failed_attempt', 'exception'))

);

CREATE TABLE production_jobs
(
    id                      VARCHAR(26) PRIMARY KEY DEFAULT generate_ulid(),
    order_item_id           VARCHAR(26) NOT NULL REFERENCES order_items (id),
    assigned_to             VARCHAR(26) REFERENCES users (id),
    status                  VARCHAR(50)             DEFAULT 'queued',
    priority                VARCHAR(50)             DEFAULT 'normal',
    quantity                INT         NOT NULL,
    notes                   TEXT,
    started_at              TIMESTAMPTZ,
    completed_at            TIMESTAMPTZ,
    estimated_completion_at TIMESTAMPTZ,
    created_at              TIMESTAMPTZ             DEFAULT NOW(),
    updated_at              TIMESTAMPTZ             DEFAULT NOW(),

    CONSTRAINT chk_production_job_status
        CHECK (status IN ('queued', 'in_progress', 'quality_check', 'completed', 'on_hold', 'cancelled')),
     CONSTRAINT chk_production_job_priority
        CHECK (priority IN ('low', 'normal', 'high', 'critical'))
);


CREATE TABLE production_updates
(
    id          VARCHAR(26) PRIMARY KEY DEFAULT generate_ulid(),
    job_id      VARCHAR(26) NOT NULL REFERENCES production_jobs (id) ON DELETE CASCADE,
    status      VARCHAR(50) NOT NULL,
    stage_name  VARCHAR(255),
    description TEXT,
    image_url   TEXT,
    posted_by   VARCHAR(26) NOT NULL REFERENCES users (id),
    created_at  TIMESTAMPTZ             DEFAULT NOW()

);

CREATE TABLE production_stages
(
    id           VARCHAR(26) PRIMARY KEY DEFAULT generate_ulid(),
    job_id       VARCHAR(26) REFERENCES production_jobs (id) ON DELETE CASCADE,
    stage_name   VARCHAR(255),
    status       VARCHAR(50),
    started_at   TIMESTAMPTZ,
    completed_at TIMESTAMPTZ,


    CONSTRAINT chk_production_stage_status
        CHECK (status IN ('pending', 'in_progress', 'completed', 'failed'))
);

-- Marketing

CREATE TABLE coupons
(
    id                  VARCHAR(26) PRIMARY KEY DEFAULT generate_ulid(),
    code                VARCHAR(100) UNIQUE NOT NULL,
    description         TEXT,
    discount_type       VARCHAR(50)         NOT NULL,
    discount_value      NUMERIC(12, 2)      NOT NULL,
    min_order_amount    NUMERIC(12, 2),
    max_discount_amount NUMERIC(12, 2),
    usage_limit         INT,
    usage_count         INT                     DEFAULT 0,
    applies_to_type     VARCHAR(50),
    applies_to_id       VARCHAR(26),
    is_active           BOOLEAN                 DEFAULT TRUE,
    starts_at           TIMESTAMPTZ,
    ends_at             TIMESTAMPTZ,
    created_at          TIMESTAMPTZ             DEFAULT NOW(),

    CONSTRAINT chk_coupon_dates
        CHECK (ends_at IS NULL OR starts_at IS NULL OR ends_at > starts_at),
    CONSTRAINT chk_coupon_discount_type
        CHECK (discount_type IN ('percentage', 'fixed_amount', 'free_shipping')),
     CONSTRAINT chk_coupon_applies_to_type
        CHECK (applies_to_type IN ('all', 'category', 'product', 'variant'))
);

CREATE TABLE coupon_usages
(
    id              VARCHAR(26) PRIMARY KEY DEFAULT generate_ulid(),
    coupon_id       VARCHAR(26)    NOT NULL REFERENCES coupons (id),
    order_id        VARCHAR(26)    NOT NULL REFERENCES orders (id),
    user_id         VARCHAR(26)    NOT NULL REFERENCES users (id),
    discount_amount NUMERIC(12, 2) NOT NULL,
    created_at      TIMESTAMPTZ             DEFAULT NOW(),

    UNIQUE (coupon_id, order_id)
);

-- Support

CREATE TABLE support_tickets
(
    id          VARCHAR(26) PRIMARY KEY DEFAULT generate_ulid(),
    user_id     VARCHAR(26)  NOT NULL REFERENCES users (id),
    order_id    VARCHAR(26) REFERENCES orders (id),
    subject     VARCHAR(255) NOT NULL,
    status      VARCHAR(50)             DEFAULT 'open',
    priority    VARCHAR(50)             DEFAULT 'normal',
    ticket_type VARCHAR(50),
    assigned_to VARCHAR(26) REFERENCES users (id),
    created_at  TIMESTAMPTZ             DEFAULT NOW(),
    updated_at  TIMESTAMPTZ             DEFAULT NOW(),
    resolved_at TIMESTAMPTZ,

    CONSTRAINT chk_ticket_status
        CHECK (status IN ('open', 'in_progress', 'pending_customer', 'resolved', 'closed')),
     CONSTRAINT chk_ticket_priority
        CHECK (priority IN ('low', 'normal', 'high', 'urgent'))
);


CREATE TABLE ticket_messages
(

    id          VARCHAR(26) PRIMARY KEY DEFAULT generate_ulid(),
    ticket_id   VARCHAR(26) NOT NULL REFERENCES support_tickets (id) ON DELETE CASCADE,
    sender_id   VARCHAR(26) NOT NULL REFERENCES users (id),
    message     TEXT        NOT NULL,
    is_internal BOOLEAN                 DEFAULT FALSE,
    attachments JSONB,
    created_at  TIMESTAMPTZ             DEFAULT NOW()
);

-- Content & System

CREATE TABLE pages
(
    id               VARCHAR(26) PRIMARY KEY DEFAULT generate_ulid(),
    title            VARCHAR(255)        NOT NULL,
    slug             VARCHAR(255) UNIQUE NOT NULL,
    content          TEXT,
    meta_title       VARCHAR(255),
    meta_description TEXT,
    status           VARCHAR(50)             DEFAULT 'draft',
    created_by       VARCHAR(26) REFERENCES users (id),
    published_at     TIMESTAMPTZ,
    created_at       TIMESTAMPTZ             DEFAULT NOW(),
    updated_at       TIMESTAMPTZ             DEFAULT NOW(),

    CONSTRAINT chk_page_status
        CHECK (status IN ('draft', 'published', 'archived'))
);

CREATE TABLE settings
(
    id          VARCHAR(26) PRIMARY KEY DEFAULT generate_ulid(),
    key         VARCHAR(255) UNIQUE NOT NULL,
    value       JSONB               NOT NULL,
    description TEXT,
    created_at  TIMESTAMPTZ             DEFAULT NOW(),
    updated_at  TIMESTAMPTZ             DEFAULT NOW()
);

CREATE TABLE conversations
(
    id              VARCHAR(26) PRIMARY KEY DEFAULT generate_ulid(),
    last_message_id VARCHAR(26),
    created_at      TIMESTAMPTZ             DEFAULT NOW(),
    updated_at      TIMESTAMPTZ             DEFAULT NOW()
);

CREATE TABLE messages
(
    id                VARCHAR(26) PRIMARY KEY    DEFAULT generate_ulid(),
    conversation_id   VARCHAR(26) REFERENCES conversations (id) ON DELETE CASCADE,
    sender_id         VARCHAR(26) REFERENCES users (id),
    message_text      TEXT,
    message_type      CITEXT CHECK ( message_type in ('text', 'image', 'video', 'file') ) DEFAULT 'text',
    parent_message_id VARCHAR(26) REFERENCES messages (id) ON DELETE SET NULL,
    file_url          VARCHAR,
    file_name         VARCHAR,
    file_size         BIGINT,
    created_at TIMESTAMPTZ DEFAULT NOW()
);

ALTER TABLE conversations ADD CONSTRAINT fk_conversations_last_message FOREIGN KEY (last_message_id) REFERENCES messages (id) ON DELETE SET NULL;

CREATE TABLE participants
(
    id              VARCHAR(26) PRIMARY KEY DEFAULT generate_ulid(),
    conversation_id VARCHAR(26) REFERENCES conversations (id) ON DELETE CASCADE,
    user_id         VARCHAR(26) REFERENCES users (id) ON DELETE CASCADE,
    joined_at       TIMESTAMPTZ,
    left_at         TIMESTAMPTZ,
    is_active       BOOLEAN                 DEFAULT FALSE
);

CREATE TABLE message_status
(
    id         VARCHAR(26) PRIMARY KEY     DEFAULT generate_ulid(),
    message_id VARCHAR(26) REFERENCES messages (id) ON DELETE CASCADE,
    user_id    VARCHAR(26) REFERENCES users (id) ON DELETE CASCADE,
    status     CITEXT CHECK ( status in ('sent', 'delivered', 'read') ) DEFAULT 'sent',
    created_at TIMESTAMPTZ                                              DEFAULT NOW(),
    updated_at TIMESTAMPTZ                                              DEFAULT NOW()
);

CREATE TABLE notifications
(
    id             VARCHAR(26) PRIMARY KEY DEFAULT generate_ulid(),
    user_id        VARCHAR(26)  NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    type           VARCHAR(100) NOT NULL,
    title          VARCHAR(255) NOT NULL,
    body           TEXT,
    action_url     TEXT,
    reference_type VARCHAR(100),
    reference_id   VARCHAR(26),
    is_read        BOOLEAN                 DEFAULT FALSE,
    read_at        TIMESTAMPTZ,
    created_at     TIMESTAMPTZ             DEFAULT NOW()
);


CREATE TABLE audit_logs
(
    id          VARCHAR(26) PRIMARY KEY DEFAULT generate_ulid(),
    actor_id    VARCHAR(26) REFERENCES users (id),
    actor_type  VARCHAR(50)  NOT NULL,
    action      VARCHAR(100) NOT NULL,
    entity_type VARCHAR(100) NOT NULL,
    entity_id   VARCHAR(26)  NOT NULL,
    changes     JSONB,
    metadata    JSONB,
    ip_address  INET,
    user_agent  TEXT,
    created_at  TIMESTAMPTZ             DEFAULT NOW()
);

CREATE TABLE outbox_events
(
    id             VARCHAR(26) PRIMARY KEY DEFAULT generate_ulid(),
    aggregate_type VARCHAR(100) NOT NULL,
    aggregate_id   VARCHAR(26)       NOT NULL,
    event_type     VARCHAR(100) NOT NULL,
    payload        JSONB        NOT NULL,
    processed      BOOLEAN                 DEFAULT FALSE,
    processed_at   TIMESTAMPTZ,
    attempts       INT                     DEFAULT 0,
    error_message  TEXT,
    created_at     TIMESTAMPTZ             DEFAULT NOW()
);

CREATE UNIQUE INDEX idx_users_unique_active_email ON users (email) WHERE deleted_at IS NULL;

CREATE INDEX idx_sessions_user_id ON sessions (user_id);

CREATE INDEX idx_sessions_expires ON sessions (expires_at);

CREATE INDEX idx_addresses_user_id ON addresses (user_id);


-- Catalog

CREATE INDEX idx_products_slug ON products (slug) WHERE deleted_at IS NULL;

CREATE INDEX idx_products_category_id ON products (category_id);

CREATE INDEX idx_products_status ON products (status);

CREATE INDEX idx_categories_slug ON categories (slug);

CREATE INDEX idx_categories_parent_id ON categories (parent_id);

CREATE INDEX idx_variants_product_id ON product_variants (product_id);

CREATE INDEX idx_variants_sku ON product_variants (sku);

CREATE INDEX idx_product_media_product_id ON product_media (product_id);


-- Inventory

CREATE INDEX idx_inventory_variant_id ON inventory (variant_id);

CREATE INDEX idx_inventory_warehouse_id ON inventory (warehouse_id);

CREATE INDEX idx_inventory_movements_variant_id ON inventory_movements (variant_id);

CREATE INDEX idx_inventory_reservations_expires ON inventory_reservations (expires_at) WHERE status = 'active';

CREATE INDEX idx_inventory_reservations_cart_id ON inventory_reservations (cart_id);


-- Sales

CREATE INDEX idx_carts_user_id ON shopping_carts (user_id);

CREATE INDEX idx_carts_guest_token ON shopping_carts (guest_token);

CREATE INDEX idx_cart_items_cart_id ON cart_items (cart_id);

CREATE INDEX idx_wishlists_user_id ON wishlists (user_id);

CREATE INDEX idx_orders_user_id ON orders (user_id);

CREATE INDEX idx_orders_order_number ON orders (order_number);

CREATE INDEX idx_orders_status ON orders (status);

CREATE INDEX idx_order_items_order_id ON order_items (order_id);

CREATE INDEX idx_order_items_product_id ON order_items (product_id);


-- Payments

CREATE INDEX idx_payments_order_id ON payments (order_id);

CREATE INDEX idx_payment_transactions_payment_id ON payment_transactions (payment_id);

CREATE INDEX idx_refunds_payment_id ON refunds (payment_id);


-- Shipping & Production

CREATE INDEX idx_shipments_order_id ON shipments (order_id);

CREATE INDEX idx_shipment_events_shipment_id ON shipment_events (shipment_id);

CREATE INDEX idx_production_jobs_order_item_id ON production_jobs (order_item_id);

CREATE INDEX idx_production_jobs_status ON production_jobs (status);


-- Marketing

CREATE INDEX idx_coupons_code ON coupons (code);

CREATE INDEX idx_coupon_usages_coupon_id ON coupon_usages (coupon_id);


-- Support

CREATE INDEX idx_tickets_user_id ON support_tickets (user_id);

CREATE INDEX idx_tickets_status ON support_tickets (status);

CREATE INDEX idx_ticket_messages_ticket_id ON ticket_messages (ticket_id);


-- System

CREATE INDEX idx_notifications_user_id_read ON notifications (user_id, is_read);

CREATE INDEX idx_notifications_created_at ON notifications (created_at);

CREATE INDEX idx_audit_entity ON audit_logs (entity_type, entity_id);

CREATE INDEX idx_audit_created_at ON audit_logs (created_at);

CREATE INDEX idx_outbox_processed ON outbox_events (processed, created_at) WHERE processed = FALSE;

CREATE INDEX idx_outbox_event_type ON outbox_events (event_type);


-- Content

CREATE INDEX idx_pages_slug ON pages (slug);

CREATE INDEX idx_settings_key ON settings (key);

