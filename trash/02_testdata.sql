-- ============================================================
-- Docker DB init: Custom Test / Seed Data
-- 10 generic inventory products / ingredients, 3 suppliers,
-- unit = base consumption / stock unit
-- supplier_product_offers.conversion_factor = units per supplier package
-- ============================================================

INSERT INTO connector_types (name, direction, description) VALUES
    ('EMAIL',    'OUTBOUND', 'Send order via email'),
    ('WHATSAPP', 'OUTBOUND', 'Send order via WhatsApp'),
    ('EDI',      'OUTBOUND', 'Send order via EDI'),
    ('WEBHOOK',  'INBOUND',  'Receive POS events via webhook'),
    ('REST_POLL','INBOUND',  'Poll POS system REST endpoint')
ON CONFLICT (name) DO NOTHING;

-- ------------------------------------------------------------
-- Suppliers
-- ------------------------------------------------------------
INSERT INTO suppliers (name, contact_email, phone) VALUES
    ('Supplier1', 'wi23b139@technikum-wien.at', '+491776254018'),
    ('Supplier2', 'wi23b139@technikum-wien.at', '+491776254018'),
    ('Supplier3', 'wi23b139@technikum-wien.at', '+491776254018');

-- ------------------------------------------------------------
-- Inventory items
-- unit = smallest practical stock / consumption unit.
-- Examples:
--   drinks are consumed as bottle
--   meat / cheese are consumed in gram
--   burger buns are consumed as piece
-- ------------------------------------------------------------
INSERT INTO inventory_items (name, unit, cached_stock, min_stock_level, reorder_target) VALUES
    ('Softdrink1 0.33L',        'bottle',  48.0,   24.0,   120.0),
    ('Softdrink2 0.50L',        'bottle',  36.0,   24.0,    96.0),
    ('Water1 0.50L',            'bottle',  72.0,   24.0,   144.0),
    ('Beer1 0.50L',             'bottle',  40.0,   20.0,   100.0),
    ('Beer2 0.33L',             'bottle',  48.0,   24.0,   120.0),
    ('Wine1 0.75L',             'bottle',  12.0,    6.0,    36.0),
    ('Wine2 0.75L',             'bottle',  18.0,    6.0,    36.0),
    ('Burger Bun',              'piece',   60.0,   30.0,   120.0),
    ('Burger Cheese',           'g',     2500.0, 1000.0,  5000.0),
    ('Burger Meat',             'g',     5000.0, 2000.0, 10000.0);

-- ------------------------------------------------------------
-- Supplier offers
-- conversion_factor = amount of base inventory units per supplier package.
-- Examples:
--   Softdrink1 0.33L: crate of 24 bottles -> 24 bottle
--   Wine 0.75L: package of 6 bottles -> 6 bottle
--   Burger Meat: ordered in 1 kg package -> 1000 g
--   Cheese: 500 g package -> 500 g
-- ------------------------------------------------------------
INSERT INTO supplier_product_offers (supplier_id, inventory_item_id, supplier_sku, unit_price, conversion_factor, is_preferred) VALUES
    ((SELECT id FROM suppliers WHERE name = 'Supplier1'), (SELECT id FROM inventory_items WHERE name = 'Softdrink1 0.33L'), 'SUP1-SOFTDRINK1-033-CRATE24', 18.00,   24.0, TRUE),
    ((SELECT id FROM suppliers WHERE name = 'Supplier1'), (SELECT id FROM inventory_items WHERE name = 'Softdrink2 0.50L'), 'SUP1-SOFTDRINK2-050-CRATE12', 13.50,   12.0, TRUE),
    ((SELECT id FROM suppliers WHERE name = 'Supplier1'), (SELECT id FROM inventory_items WHERE name = 'Water1 0.50L'),     'SUP1-WATER1-050-CRATE24',     10.00,   24.0, TRUE),
    ((SELECT id FROM suppliers WHERE name = 'Supplier1'), (SELECT id FROM inventory_items WHERE name = 'Beer1 0.50L'),      'SUP1-BEER1-050-CRATE20',      22.00,   20.0, TRUE),
    ((SELECT id FROM suppliers WHERE name = 'Supplier1'), (SELECT id FROM inventory_items WHERE name = 'Beer2 0.33L'),      'SUP1-BEER2-033-CRATE24',      24.00,   24.0, TRUE),

    ((SELECT id FROM suppliers WHERE name = 'Supplier2'), (SELECT id FROM inventory_items WHERE name = 'Wine1 0.75L'),      'SUP2-WINE1-075-PACK6',        36.00,    6.0, TRUE),
    ((SELECT id FROM suppliers WHERE name = 'Supplier2'), (SELECT id FROM inventory_items WHERE name = 'Wine2 0.75L'),      'SUP2-WINE2-075-PACK6',        42.00,    6.0, TRUE),
    ((SELECT id FROM suppliers WHERE name = 'Supplier2'), (SELECT id FROM inventory_items WHERE name = 'Burger Bun'),       'SUP2-BURGER-BUN-PACK30',      15.00,   30.0, TRUE),
    ((SELECT id FROM suppliers WHERE name = 'Supplier2'), (SELECT id FROM inventory_items WHERE name = 'Burger Cheese'),    'SUP2-BURGER-CHEESE-500G',      8.00,  500.0, TRUE),
    ((SELECT id FROM suppliers WHERE name = 'Supplier2'), (SELECT id FROM inventory_items WHERE name = 'Burger Meat'),      'SUP2-BURGER-MEAT-1KG',        18.00, 1000.0, TRUE);

-- ------------------------------------------------------------
-- Active connector configs
-- Requirement: two active EMAIL connectors, one active WHATSAPP connector.
-- The schema allows only one active connector per supplier.
-- ------------------------------------------------------------
INSERT INTO connector_configs (supplier_id, connector_type_id, config_payload, is_active) VALUES
    ((SELECT id FROM suppliers WHERE name = 'Supplier1'),
     (SELECT id FROM connector_types WHERE name = 'EMAIL'),
     '{"recipientEmail":"wi23b139@technikum-wien.at","subjectTemplate":"Inventory Order - {supplierName} - {date}","bodyTemplate":"Dear {supplierName},\n\nPlease process the following order:\n\n{orderLines}\n\nThank you."}',
     TRUE),
    ((SELECT id FROM suppliers WHERE name = 'Supplier2'),
     (SELECT id FROM connector_types WHERE name = 'EMAIL'),
     '{"recipientEmail":"wi23b139@technikum-wien.at","subjectTemplate":"Inventory Order - {supplierName} - {date}","bodyTemplate":"Dear {supplierName},\n\nPlease process the following order:\n\n{orderLines}\n\nThank you."}',
     TRUE),
    ((SELECT id FROM suppliers WHERE name = 'Supplier3'),
     (SELECT id FROM connector_types WHERE name = 'WHATSAPP'),
     '{"recipientPhone":"+491776254018","messageTemplate":"Order {date} - {supplierName}:\n\n{orderLines}"}',
     TRUE);

-- ------------------------------------------------------------
-- Sales products
-- Includes direct drink products and one composite Burger product.
-- Only direct drink products and one composite Burger product are included.
-- ------------------------------------------------------------
INSERT INTO sales_products (external_id, name, pos_system) VALUES
    ('SOFTDRINK1-033', 'Softdrink1 0.33L', 'TestPOS'),
    ('SOFTDRINK2-050', 'Softdrink2 0.50L', 'TestPOS'),
    ('WATER1-050',     'Water1 0.50L',     'TestPOS'),
    ('BEER1-050',      'Beer1 0.50L',      'TestPOS'),
    ('BEER2-033',      'Beer2 0.33L',      'TestPOS'),
    ('WINE1-075',      'Wine1 0.75L',      'TestPOS'),
    ('WINE2-075',      'Wine2 0.75L',      'TestPOS'),
    ('BURGER-001',     'Burger',           'TestPOS');

-- Direct sales-product to inventory-item mappings for single-item products.
-- qty_per_sale is in the inventory item base unit.
INSERT INTO pos_product_mappings (sales_product_id, inventory_item_id, qty_per_sale) VALUES
    ((SELECT id FROM sales_products WHERE external_id = 'SOFTDRINK1-033'), (SELECT id FROM inventory_items WHERE name = 'Softdrink1 0.33L'), 1.0),
    ((SELECT id FROM sales_products WHERE external_id = 'SOFTDRINK2-050'), (SELECT id FROM inventory_items WHERE name = 'Softdrink2 0.50L'), 1.0),
    ((SELECT id FROM sales_products WHERE external_id = 'WATER1-050'),     (SELECT id FROM inventory_items WHERE name = 'Water1 0.50L'),     1.0),
    ((SELECT id FROM sales_products WHERE external_id = 'BEER1-050'),      (SELECT id FROM inventory_items WHERE name = 'Beer1 0.50L'),      1.0),
    ((SELECT id FROM sales_products WHERE external_id = 'BEER2-033'),      (SELECT id FROM inventory_items WHERE name = 'Beer2 0.33L'),      1.0),
    ((SELECT id FROM sales_products WHERE external_id = 'WINE1-075'),      (SELECT id FROM inventory_items WHERE name = 'Wine1 0.75L'),      1.0),
    ((SELECT id FROM sales_products WHERE external_id = 'WINE2-075'),      (SELECT id FROM inventory_items WHERE name = 'Wine2 0.75L'),      1.0);

-- Composite products: each qty_required is in the inventory item base unit.
-- Burger: 1 bun, 30 g cheese, 150 g meat.
INSERT INTO product_components (sales_product_id, inventory_item_id, qty_required) VALUES
    ((SELECT id FROM sales_products WHERE external_id = 'BURGER-001'), (SELECT id FROM inventory_items WHERE name = 'Burger Bun'),      1.0),
    ((SELECT id FROM sales_products WHERE external_id = 'BURGER-001'), (SELECT id FROM inventory_items WHERE name = 'Burger Cheese'),  30.0),
    ((SELECT id FROM sales_products WHERE external_id = 'BURGER-001'), (SELECT id FROM inventory_items WHERE name = 'Burger Meat'),   150.0);

-- Initial stock transactions matching cached_stock values.
-- Deltas are stored in the same base unit as inventory_items.unit.
INSERT INTO inventory_transactions (inventory_item_id, transaction_type, delta, reference_id) VALUES
    ((SELECT id FROM inventory_items WHERE name = 'Softdrink1 0.33L'),  'GOODS_RECEIPT',   48.0, 'INIT-SOFTDRINK1-033'),
    ((SELECT id FROM inventory_items WHERE name = 'Softdrink2 0.50L'),  'GOODS_RECEIPT',   36.0, 'INIT-SOFTDRINK2-050'),
    ((SELECT id FROM inventory_items WHERE name = 'Water1 0.50L'),      'GOODS_RECEIPT',   72.0, 'INIT-WATER1-050'),
    ((SELECT id FROM inventory_items WHERE name = 'Beer1 0.50L'),       'GOODS_RECEIPT',   40.0, 'INIT-BEER1-050'),
    ((SELECT id FROM inventory_items WHERE name = 'Beer2 0.33L'),       'GOODS_RECEIPT',   48.0, 'INIT-BEER2-033'),
    ((SELECT id FROM inventory_items WHERE name = 'Wine1 0.75L'),       'GOODS_RECEIPT',   12.0, 'INIT-WINE1-075'),
    ((SELECT id FROM inventory_items WHERE name = 'Wine2 0.75L'),       'GOODS_RECEIPT',   18.0, 'INIT-WINE2-075'),
    ((SELECT id FROM inventory_items WHERE name = 'Burger Bun'),        'GOODS_RECEIPT',   60.0, 'INIT-BURGER-BUN'),
    ((SELECT id FROM inventory_items WHERE name = 'Burger Cheese'),     'GOODS_RECEIPT', 2500.0, 'INIT-BURGER-CHEESE'),
    ((SELECT id FROM inventory_items WHERE name = 'Burger Meat'),       'GOODS_RECEIPT', 5000.0, 'INIT-BURGER-MEAT');
