-- ============================================================
-- Docker DB init: ready2order inbound connector seed data
-- ============================================================

INSERT INTO connector_types (name, direction, description) VALUES
    ('READY2ORDER_POLLING', 'INBOUND', 'Poll ready2order REST API for invoices'),
    ('READY2ORDER_WEBHOOK', 'INBOUND', 'Receive ready2order webhook events')
ON CONFLICT (name) DO NOTHING;

INSERT INTO app_settings (setting_key, setting_value) VALUES
    ('r2o.accountToken',          ''),
    ('r2o.developerToken',        ''),
    ('r2o.pollingIntervalMinutes','5'),
    ('r2o.posSource',             'READY2ORDER'),
    ('r2o.lastPolledAt',          ''),
    ('r2o.enabled',               'false'),
    ('r2o.inboundMode',           'polling')
ON CONFLICT (setting_key) DO NOTHING;

-- ------------------------------------------------------------
-- Sales products with pos_system=READY2ORDER
-- external_id matches ready2order product_id
-- ------------------------------------------------------------
INSERT INTO sales_products (external_id, name, pos_system) VALUES
    ('10001', 'Cola 0.33L',      'READY2ORDER'),
    ('10002', 'Cola 0.50L',      'READY2ORDER'),
    ('10003', 'Water 0.50L',     'READY2ORDER'),
    ('10004', 'Beer 0.50L',      'READY2ORDER'),
    ('10005', 'Beer 0.33L',      'READY2ORDER'),
    ('10006', 'Wine 0.75L',      'READY2ORDER'),
    ('10007', 'Cheeseburger',    'READY2ORDER'),
    ('10008', 'Hamburger',       'READY2ORDER')
ON CONFLICT (external_id) DO NOTHING;

-- ------------------------------------------------------------
-- Product components: link sales products to inventory items
-- ------------------------------------------------------------
INSERT INTO product_components (sales_product_id, inventory_item_id, qty_required)
SELECT sp.id, ii.id, 1.0
FROM sales_products sp, inventory_items ii
WHERE sp.external_id = '10001' AND ii.name = 'Softdrink1 0.33L'
ON CONFLICT DO NOTHING;

INSERT INTO product_components (sales_product_id, inventory_item_id, qty_required)
SELECT sp.id, ii.id, 1.0
FROM sales_products sp, inventory_items ii
WHERE sp.external_id = '10002' AND ii.name = 'Softdrink2 0.50L'
ON CONFLICT DO NOTHING;

INSERT INTO product_components (sales_product_id, inventory_item_id, qty_required)
SELECT sp.id, ii.id, 1.0
FROM sales_products sp, inventory_items ii
WHERE sp.external_id = '10003' AND ii.name = 'Water1 0.50L'
ON CONFLICT DO NOTHING;

INSERT INTO product_components (sales_product_id, inventory_item_id, qty_required)
SELECT sp.id, ii.id, 1.0
FROM sales_products sp, inventory_items ii
WHERE sp.external_id = '10004' AND ii.name = 'Beer1 0.50L'
ON CONFLICT DO NOTHING;

INSERT INTO product_components (sales_product_id, inventory_item_id, qty_required)
SELECT sp.id, ii.id, 1.0
FROM sales_products sp, inventory_items ii
WHERE sp.external_id = '10005' AND ii.name = 'Beer2 0.33L'
ON CONFLICT DO NOTHING;

INSERT INTO product_components (sales_product_id, inventory_item_id, qty_required)
SELECT sp.id, ii.id, 1.0
FROM sales_products sp, inventory_items ii
WHERE sp.external_id = '10006' AND ii.name = 'Wine1 0.75L'
ON CONFLICT DO NOTHING;

INSERT INTO product_components (sales_product_id, inventory_item_id, qty_required)
SELECT sp.id, ii.id, 1.0
FROM sales_products sp, inventory_items ii
WHERE sp.external_id = '10007' AND ii.name = 'Burger Bun'
ON CONFLICT DO NOTHING;

INSERT INTO product_components (sales_product_id, inventory_item_id, qty_required)
SELECT sp.id, ii.id, 30.0
FROM sales_products sp, inventory_items ii
WHERE sp.external_id = '10007' AND ii.name = 'Burger Cheese'
ON CONFLICT DO NOTHING;

INSERT INTO product_components (sales_product_id, inventory_item_id, qty_required)
SELECT sp.id, ii.id, 150.0
FROM sales_products sp, inventory_items ii
WHERE sp.external_id = '10007' AND ii.name = 'Burger Meat'
ON CONFLICT DO NOTHING;

INSERT INTO product_components (sales_product_id, inventory_item_id, qty_required)
SELECT sp.id, ii.id, 1.0
FROM sales_products sp, inventory_items ii
WHERE sp.external_id = '10008' AND ii.name = 'Burger Bun'
ON CONFLICT DO NOTHING;

INSERT INTO product_components (sales_product_id, inventory_item_id, qty_required)
SELECT sp.id, ii.id, 150.0
FROM sales_products sp, inventory_items ii
WHERE sp.external_id = '10008' AND ii.name = 'Burger Meat'
ON CONFLICT DO NOTHING;
