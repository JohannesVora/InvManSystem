-- ============================================================
-- V2: Seed / Test Data
-- ============================================================

-- Connector Types
INSERT INTO connector_types (name, direction, description) VALUES
    ('EMAIL',    'OUTBOUND', 'Send order via email'),
    ('WHATSAPP', 'OUTBOUND', 'Send order via WhatsApp'),
    ('REST',     'OUTBOUND', 'Send order via REST API'),
    ('EDI',      'OUTBOUND', 'Send order via EDI'),
    ('WEBHOOK',  'INBOUND',  'Receive POS events via webhook'),
    ('REST_POLL','INBOUND',  'Poll POS system REST endpoint');

-- Suppliers
INSERT INTO suppliers (name, contact_email, phone) VALUES
    ('Fresh Farm Co.',   'orders@freshfarm.com', '+1-555-0101'),
    ('Bakery Supplies Ltd.', 'supply@bakerysupplies.com', '+1-555-0202'),
    ('Dairy Direct',     'orders@dairydirect.com', '+1-555-0303');

-- Inventory Items
INSERT INTO inventory_items (name, unit, cached_stock, min_stock_level, reorder_target) VALUES
    ('Flour',         'kg',    50.0,  20.0, 100.0),
    ('Sugar',         'kg',    30.0,  15.0,  60.0),
    ('Butter',        'kg',     8.0,  10.0,  30.0),
    ('Milk',          'liter', 15.0,  20.0,  50.0),
    ('Eggs',          'dozen',  4.0,   5.0,  20.0),
    ('Vanilla Extract','ml',   50.0,  30.0, 100.0);

-- Supplier Product Offers
INSERT INTO supplier_product_offers (supplier_id, inventory_item_id, supplier_sku, unit_price, conversion_factor, is_preferred) VALUES
    (2, 1, 'FLOUR-25KG', 18.50, 25.0, 1),   -- Bakery Supplies: Flour in 25kg bags
    (2, 2, 'SUGAR-10KG', 12.00, 10.0, 1),   -- Bakery Supplies: Sugar in 10kg bags
    (3, 3, 'BUTTER-1KG',  6.50,  1.0, 1),   -- Dairy Direct: Butter in 1kg blocks
    (3, 4, 'MILK-10L',    8.00, 10.0, 1),   -- Dairy Direct: Milk in 10L containers
    (1, 5, 'EGGS-30',     5.50,  2.5, 1),   -- Fresh Farm: Eggs in 30-pack (2.5 dozen)
    (2, 6, 'VANILLA-100', 3.20,  0.1, 1);   -- Bakery Supplies: Vanilla in 100ml bottles

-- Connector Configs
INSERT INTO connector_configs (supplier_id, connector_type_id, config_payload, is_active) VALUES
    (1, 1, '{"recipientEmail":"orders@freshfarm.com","subjectTemplate":"Inventory Order - {supplierName} - {date}","bodyTemplate":"Dear {supplierName},\n\nPlease process the following order:\n\n{orderLines}\n\nThank you."}', 1),
    (2, 1, '{"recipientEmail":"supply@bakerysupplies.com","subjectTemplate":"Inventory Order - {supplierName} - {date}","bodyTemplate":"Dear {supplierName},\n\nPlease process the following order:\n\n{orderLines}\n\nThank you."}', 1),
    (3, 1, '{"recipientEmail":"orders@dairydirect.com","subjectTemplate":"Inventory Order - {supplierName} - {date}","bodyTemplate":"Dear {supplierName},\n\nPlease process the following order:\n\n{orderLines}\n\nThank you."}', 1);

-- Sales Products
INSERT INTO sales_products (external_id, name, pos_system) VALUES
    ('CAKE-001', 'Birthday Cake', 'SquarePOS'),
    ('BREAD-001', 'Sourdough Bread', 'SquarePOS'),
    ('PIE-001', 'Apple Pie', 'SquarePOS');

-- Initial Transactions (to set baseline stock)
INSERT INTO inventory_transactions (inventory_item_id, transaction_type, delta, reference_id) VALUES
    (1, 'GOODS_RECEIPT', 50.0,  'INIT-001'),
    (2, 'GOODS_RECEIPT', 30.0,  'INIT-002'),
    (3, 'GOODS_RECEIPT',  8.0,  'INIT-003'),
    (4, 'GOODS_RECEIPT', 15.0,  'INIT-004'),
    (5, 'GOODS_RECEIPT',  4.0,  'INIT-005'),
    (6, 'GOODS_RECEIPT', 50.0,  'INIT-006');
