-- ============================================================
-- Docker DB init: Schema (mirrors Flyway V1__init.sql)
-- Only runs on a fresh empty volume; Flyway handles migrations
-- when Spring Boot starts.
-- ============================================================

CREATE TABLE IF NOT EXISTS sales_products (
    id          BIGSERIAL PRIMARY KEY,
    external_id TEXT    NOT NULL UNIQUE,
    name        TEXT    NOT NULL,
    pos_system  TEXT
);

CREATE TABLE IF NOT EXISTS inventory_items (
    id                   BIGSERIAL PRIMARY KEY,
    name                 TEXT             NOT NULL,
    unit                 TEXT,
    cached_stock         DOUBLE PRECISION DEFAULT 0,
    min_stock_level      DOUBLE PRECISION DEFAULT 0,
    reorder_target       DOUBLE PRECISION DEFAULT 0,
    last_recalculated_at TIMESTAMP
);

CREATE TABLE IF NOT EXISTS pos_product_mappings (
    id                BIGSERIAL PRIMARY KEY,
    sales_product_id  BIGINT NOT NULL REFERENCES sales_products(id),
    inventory_item_id BIGINT NOT NULL REFERENCES inventory_items(id),
    qty_per_sale      DOUBLE PRECISION NOT NULL
);

CREATE TABLE IF NOT EXISTS product_components (
    id                BIGSERIAL PRIMARY KEY,
    sales_product_id  BIGINT NOT NULL REFERENCES sales_products(id),
    inventory_item_id BIGINT NOT NULL REFERENCES inventory_items(id),
    qty_required      DOUBLE PRECISION NOT NULL
);

CREATE TABLE IF NOT EXISTS inventory_transactions (
    id                BIGSERIAL PRIMARY KEY,
    inventory_item_id BIGINT NOT NULL REFERENCES inventory_items(id),
    transaction_type  TEXT   NOT NULL,
    delta             DOUBLE PRECISION NOT NULL,
    reference_id      TEXT,
    created_at        TIMESTAMP DEFAULT NOW()
);

CREATE TABLE IF NOT EXISTS suppliers (
    id            BIGSERIAL PRIMARY KEY,
    name          TEXT NOT NULL,
    contact_email TEXT,
    phone         TEXT
);

CREATE TABLE IF NOT EXISTS supplier_product_offers (
    id                     BIGSERIAL PRIMARY KEY,
    supplier_id            BIGINT NOT NULL REFERENCES suppliers(id),
    inventory_item_id      BIGINT REFERENCES inventory_items(id),
    supplier_sku           TEXT,
    supplier_product_name  TEXT,
    package_unit           TEXT,
    unit_price             DOUBLE PRECISION,
    conversion_factor      DOUBLE PRECISION DEFAULT 1.0,
    is_preferred           BOOLEAN NOT NULL DEFAULT FALSE
);

CREATE UNIQUE INDEX IF NOT EXISTS uidx_preferred_offer
    ON supplier_product_offers(inventory_item_id)
    WHERE is_preferred = TRUE;

CREATE TABLE IF NOT EXISTS replenishment_orders (
    id           BIGSERIAL PRIMARY KEY,
    status       TEXT      NOT NULL DEFAULT 'DRAFT',
    created_at   TIMESTAMP DEFAULT NOW(),
    submitted_at TIMESTAMP,
    received_at  TIMESTAMP DEFAULT NULL
);

CREATE TABLE IF NOT EXISTS replenishment_order_lines (
    id                     BIGSERIAL PRIMARY KEY,
    replenishment_order_id BIGINT NOT NULL REFERENCES replenishment_orders(id),
    inventory_item_id      BIGINT NOT NULL REFERENCES inventory_items(id),
    requested_qty          DOUBLE PRECISION NOT NULL,
    received_qty           DOUBLE PRECISION DEFAULT NULL
);

CREATE TABLE IF NOT EXISTS connector_types (
    id          BIGSERIAL PRIMARY KEY,
    name        TEXT NOT NULL UNIQUE,
    direction   TEXT NOT NULL,
    description TEXT
);

CREATE TABLE IF NOT EXISTS connector_configs (
    id                BIGSERIAL PRIMARY KEY,
    supplier_id       BIGINT NOT NULL REFERENCES suppliers(id),
    connector_type_id BIGINT NOT NULL REFERENCES connector_types(id),
    config_payload    TEXT,
    is_active         BOOLEAN NOT NULL DEFAULT TRUE
);

CREATE UNIQUE INDEX IF NOT EXISTS uidx_active_connector
    ON connector_configs(supplier_id)
    WHERE is_active = TRUE;

CREATE TABLE IF NOT EXISTS supplier_orders (
    id                     BIGSERIAL PRIMARY KEY,
    replenishment_order_id BIGINT NOT NULL REFERENCES replenishment_orders(id),
    supplier_id            BIGINT NOT NULL REFERENCES suppliers(id),
    status                 TEXT   NOT NULL DEFAULT 'PENDING',
    created_at             TIMESTAMP DEFAULT NOW()
);

CREATE TABLE IF NOT EXISTS supplier_order_lines (
    id                BIGSERIAL PRIMARY KEY,
    supplier_order_id BIGINT NOT NULL REFERENCES supplier_orders(id),
    inventory_item_id BIGINT NOT NULL REFERENCES inventory_items(id),
    ordered_qty       DOUBLE PRECISION NOT NULL,
    supplier_sku      TEXT
);

CREATE TABLE IF NOT EXISTS connector_executions (
    id                  BIGSERIAL PRIMARY KEY,
    supplier_order_id   BIGINT NOT NULL REFERENCES supplier_orders(id),
    connector_config_id BIGINT NOT NULL REFERENCES connector_configs(id),
    status              TEXT   NOT NULL,
    error_message       TEXT,
    executed_at         TIMESTAMP DEFAULT NOW()
);

CREATE TABLE IF NOT EXISTS app_settings (
    id            BIGSERIAL PRIMARY KEY,
    setting_key   TEXT NOT NULL UNIQUE,
    setting_value TEXT
);

INSERT INTO app_settings (setting_key, setting_value) VALUES
    ('smtp.host',              'smtp.gmail.com'),
    ('smtp.port',              '465'),
    ('smtp.username',          ''),
    ('smtp.password',          ''),
    ('smtp.from',              ''),
    ('wppconnect.baseUrl',     'http://wppconnect:21465'),
    ('wppconnect.secretKey',   'THISISMYSECURETOKEN'),
    ('wppconnect.session',     'inventory-session'),
    ('wppconnect.token',       '')
ON CONFLICT (setting_key) DO NOTHING;
