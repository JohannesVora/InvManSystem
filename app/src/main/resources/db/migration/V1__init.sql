-- ============================================================
-- V1: Initial Schema
-- ============================================================

CREATE TABLE IF NOT EXISTS sales_products (
    id          INTEGER PRIMARY KEY AUTOINCREMENT,
    external_id TEXT    NOT NULL UNIQUE,
    name        TEXT    NOT NULL,
    pos_system  TEXT
);

CREATE TABLE IF NOT EXISTS inventory_items (
    id                   INTEGER PRIMARY KEY AUTOINCREMENT,
    name                 TEXT    NOT NULL,
    unit                 TEXT,
    cached_stock         REAL    DEFAULT 0,
    min_stock_level      REAL    DEFAULT 0,
    reorder_target       REAL    DEFAULT 0,
    last_recalculated_at TEXT
);

CREATE TABLE IF NOT EXISTS pos_product_mappings (
    id                INTEGER PRIMARY KEY AUTOINCREMENT,
    sales_product_id  INTEGER NOT NULL REFERENCES sales_products(id),
    inventory_item_id INTEGER NOT NULL REFERENCES inventory_items(id),
    qty_per_sale      REAL    NOT NULL
);

CREATE TABLE IF NOT EXISTS product_components (
    id                INTEGER PRIMARY KEY AUTOINCREMENT,
    sales_product_id  INTEGER NOT NULL REFERENCES sales_products(id),
    inventory_item_id INTEGER NOT NULL REFERENCES inventory_items(id),
    qty_required      REAL    NOT NULL
);

CREATE TABLE IF NOT EXISTS inventory_transactions (
    id                INTEGER PRIMARY KEY AUTOINCREMENT,
    inventory_item_id INTEGER NOT NULL REFERENCES inventory_items(id),
    transaction_type  TEXT    NOT NULL,
    delta             REAL    NOT NULL,
    reference_id      TEXT,
    created_at        TEXT    DEFAULT (datetime('now'))
);

CREATE TABLE IF NOT EXISTS suppliers (
    id            INTEGER PRIMARY KEY AUTOINCREMENT,
    name          TEXT    NOT NULL,
    contact_email TEXT,
    phone         TEXT
);

CREATE TABLE IF NOT EXISTS supplier_product_offers (
    id                INTEGER PRIMARY KEY AUTOINCREMENT,
    supplier_id       INTEGER NOT NULL REFERENCES suppliers(id),
    inventory_item_id INTEGER NOT NULL REFERENCES inventory_items(id),
    supplier_sku      TEXT,
    unit_price        REAL,
    conversion_factor REAL    DEFAULT 1.0,
    is_preferred      INTEGER DEFAULT 0
);

CREATE UNIQUE INDEX IF NOT EXISTS uidx_preferred_offer
    ON supplier_product_offers(inventory_item_id)
    WHERE is_preferred = 1;

CREATE TABLE IF NOT EXISTS replenishment_orders (
    id           INTEGER PRIMARY KEY AUTOINCREMENT,
    status       TEXT    NOT NULL DEFAULT 'DRAFT',
    created_at   TEXT    DEFAULT (datetime('now')),
    submitted_at TEXT
);

CREATE TABLE IF NOT EXISTS replenishment_order_lines (
    id                      INTEGER PRIMARY KEY AUTOINCREMENT,
    replenishment_order_id  INTEGER NOT NULL REFERENCES replenishment_orders(id),
    inventory_item_id       INTEGER NOT NULL REFERENCES inventory_items(id),
    requested_qty           REAL    NOT NULL
);

CREATE TABLE IF NOT EXISTS connector_types (
    id          INTEGER PRIMARY KEY AUTOINCREMENT,
    name        TEXT    NOT NULL UNIQUE,
    direction   TEXT    NOT NULL,
    description TEXT
);

CREATE TABLE IF NOT EXISTS connector_configs (
    id                 INTEGER PRIMARY KEY AUTOINCREMENT,
    supplier_id        INTEGER NOT NULL REFERENCES suppliers(id),
    connector_type_id  INTEGER NOT NULL REFERENCES connector_types(id),
    config_payload     TEXT,
    is_active          INTEGER DEFAULT 1
);

CREATE UNIQUE INDEX IF NOT EXISTS uidx_active_connector
    ON connector_configs(supplier_id)
    WHERE is_active = 1;

CREATE TABLE IF NOT EXISTS supplier_orders (
    id                      INTEGER PRIMARY KEY AUTOINCREMENT,
    replenishment_order_id  INTEGER NOT NULL REFERENCES replenishment_orders(id),
    supplier_id             INTEGER NOT NULL REFERENCES suppliers(id),
    status                  TEXT    NOT NULL DEFAULT 'PENDING',
    created_at              TEXT    DEFAULT (datetime('now'))
);

CREATE TABLE IF NOT EXISTS supplier_order_lines (
    id               INTEGER PRIMARY KEY AUTOINCREMENT,
    supplier_order_id INTEGER NOT NULL REFERENCES supplier_orders(id),
    inventory_item_id INTEGER NOT NULL REFERENCES inventory_items(id),
    ordered_qty       REAL    NOT NULL,
    supplier_sku      TEXT
);

CREATE TABLE IF NOT EXISTS connector_executions (
    id                  INTEGER PRIMARY KEY AUTOINCREMENT,
    supplier_order_id   INTEGER NOT NULL REFERENCES supplier_orders(id),
    connector_config_id INTEGER NOT NULL REFERENCES connector_configs(id),
    status              TEXT    NOT NULL,
    error_message       TEXT,
    executed_at         TEXT    DEFAULT (datetime('now'))
);
