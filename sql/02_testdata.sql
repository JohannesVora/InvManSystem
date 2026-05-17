-- ============================================================
-- Docker DB init: Test Data – El Reno OG Usability Test
-- ============================================================
-- Supplier 1: Transgourmet       → EMAIL  wi23b139@technikum-wien.at
-- Supplier 2: Joseph Brot        → EMAIL  wi23b139@technikum-wien.at (Joseph Brot Vorlage)
-- Supplier 3: Fleischhandel Meat → WHATSAPP 00491776284018
-- ============================================================
-- Canonical Units:
--   Stückware (Salat, Eier, Brot, Rolls)  → piece
--   Gewicht   (Käse, Saucen kg, Gewürze)  → g
--   Volumen   (Öl, Saucen l)              → ml
--   Gemüse lose nach Gewicht              → kg
-- ============================================================
-- Low-Stock Produkte (cached_stock < min_stock_level) – 12 von 24:
--   Jalapeños, Knoblauch, Martins Potato Rolls,
--   Dijon Senf, Frenchs Yellow Mustard, Kikkoman Soja Sauce,
--   Dallmayr Prodomo Kaffee, Wiberg Pfeffer,
--   Flander Cheddar, Bio Joseph Brot Wecken, Bio La Marianne, Faschiertes
-- ============================================================

-- ------------------------------------------------------------
-- 1. Connector Types
-- ------------------------------------------------------------
INSERT INTO connector_types (name, direction, description) VALUES
    ('EMAIL',    'OUTBOUND', 'Send order via email'),
    ('WHATSAPP', 'OUTBOUND', 'Send order via WhatsApp'),
    ('EDI',      'OUTBOUND', 'Send order via EDI'),
    ('WEBHOOK',  'INBOUND',  'Receive POS events via webhook'),
    ('REST_POLL','INBOUND',  'Poll POS system REST endpoint')
ON CONFLICT (name) DO NOTHING;

-- ------------------------------------------------------------
-- 2. Suppliers
-- ------------------------------------------------------------
INSERT INTO suppliers (name, contact_email, phone) VALUES
    ('Transgourmet',       'wi23b139@technikum-wien.at', NULL),
    ('Joseph Brot',        'wi23b139@technikum-wien.at', NULL),
    ('Fleischhandel Meat', NULL,                         '00491776284018');

-- ------------------------------------------------------------
-- 3. Inventory Items
-- ★ = Low Stock (cached_stock < min_stock_level)
-- ------------------------------------------------------------
INSERT INTO inventory_items (name, unit, cached_stock, min_stock_level, reorder_target) VALUES

    -- Transgourmet: Gemüse / Salate
    ('Zwiebel weiss',                'kg',    10.0,    5.0,   25.0),   -- OK
    ('Eisbergsalat',                 'piece', 20.0,   10.0,   40.0),   -- OK
    ('Gurken',                       'piece', 24.0,   12.0,   48.0),   -- OK
    ('Jalapeños',                    'kg',     0.5,    1.0,    5.0),   -- ★ LOW
    ('Knoblauch',                    'g',    100.0,  200.0, 1000.0),   -- ★ LOW

    -- Transgourmet: Tiefkühl
    ('Aviko Pommes Super Crunch 7mm','kg',    10.0,    5.0,   25.0),   -- OK
    ('Martins Potato Rolls 4 Inch',  'piece', 20.0,   48.0,  192.0),   -- ★ LOW

    -- Transgourmet: Saucen & Condiments
    ('Heinz Tomato Ketchup',         'g',  11500.0, 5750.0, 23000.0), -- OK
    ('Hellmanns Mayonnaise',         'ml',  5000.0, 2500.0, 10000.0), -- OK
    ('Dijon Senf',                   'g',   1000.0, 2500.0, 10000.0), -- ★ LOW
    ('Frenchs Yellow Mustard',       'g',    500.0, 1490.0,  5960.0), -- ★ LOW
    ('Kikkoman Soja Sauce',          'ml',   400.0,  950.0,  3800.0), -- ★ LOW
    ('Mississippi BBQ Sauce Sweet',  'g',   1814.0,  907.0,  3628.0), -- OK

    -- Transgourmet: Grundzutaten
    ('Dallmayr Prodomo Kaffee',      'g',    200.0,  500.0,  2000.0), -- ★ LOW
    ('Wiener Zucker Feinkristall',   'g',  10000.0, 5000.0, 20000.0), -- OK
    ('Economy Sonnenblumenöl',       'ml', 10000.0, 5000.0, 20000.0), -- OK
    ('Bad Ischler Tafelsalz',        'g',  10000.0, 5000.0, 20000.0), -- OK
    ('Wiberg Pfeffer schwarz ganz',  'g',     80.0,  200.0,  1500.0), -- ★ LOW

    -- Transgourmet: Käse & Eier
    ('Flander Cheddar Scheiben',     'g',    500.0, 1000.0,  4000.0), -- ★ LOW
    ('Bodenhaltungseier Gr L',       'piece', 60.0,   30.0,  120.0),  -- OK

    -- Joseph Brot
    ('Bio Joseph Brot Wecken 2kg TK',    'piece',  1.0,  3.0,  12.0), -- ★ LOW
    ('Bio Waldviertler Erdäpfelbrot TK', 'piece', 10.0,  6.0,  24.0), -- OK
    ('Bio La Marianne TK',               'piece',  2.0,  3.0,  12.0), -- ★ LOW

    -- Fleischhandel Meat
    ('Faschiertes',                  'g',    800.0, 2000.0,  6000.0); -- ★ LOW

-- ------------------------------------------------------------
-- 4. Supplier Product Offers
-- conversion_factor = Anzahl Basis-Einheiten pro Liefergebinde
-- ------------------------------------------------------------
INSERT INTO supplier_product_offers
    (supplier_id, inventory_item_id, supplier_sku, unit_price, conversion_factor, is_preferred)
VALUES
    -- Transgourmet: Gemüse
    -- Zwiebel: EH = 5 kg → 5.0 kg pro Gebinde
    ((SELECT id FROM suppliers WHERE name = 'Transgourmet'),
     (SELECT id FROM inventory_items WHERE name = 'Zwiebel weiss'),
     'TG-45294',    1.89,     5.0, TRUE),

    -- Eisbergsalat: KT = 10 ST → 10 Stück
    ((SELECT id FROM suppliers WHERE name = 'Transgourmet'),
     (SELECT id FROM inventory_items WHERE name = 'Eisbergsalat'),
     'TG-622530',   2.19,    10.0, TRUE),

    -- Gurken: KT = 12 ST → 12 Stück
    ((SELECT id FROM suppliers WHERE name = 'Transgourmet'),
     (SELECT id FROM inventory_items WHERE name = 'Gurken'),
     'TG-903302',   1.11,    12.0, TRUE),

    -- Jalapeños: EH = 1 kg → 1.0 kg
    ((SELECT id FROM suppliers WHERE name = 'Transgourmet'),
     (SELECT id FROM inventory_items WHERE name = 'Jalapeños'),
     'TG-1702901', 15.99,     1.0, TRUE),

    -- Knoblauch: KT = 10 PK à 200 g → 2000 g
    ((SELECT id FROM suppliers WHERE name = 'Transgourmet'),
     (SELECT id FROM inventory_items WHERE name = 'Knoblauch'),
     'TG-1271816',  2.19,  2000.0, TRUE),

    -- Transgourmet: Tiefkühl
    -- Pommes: KT = 4 PK à 2,5 kg → 10 kg
    ((SELECT id FROM suppliers WHERE name = 'Transgourmet'),
     (SELECT id FROM inventory_items WHERE name = 'Aviko Pommes Super Crunch 7mm'),
     'TG-1261668',  7.31,    10.0, TRUE),

    -- Potato Rolls: KT = 4 × 12 → 48 Stück
    ((SELECT id FROM suppliers WHERE name = 'Transgourmet'),
     (SELECT id FROM inventory_items WHERE name = 'Martins Potato Rolls 4 Inch'),
     'TG-3868262', 33.26,    48.0, TRUE),

    -- Transgourmet: Saucen & Condiments
    -- Ketchup: ST = 11,5 kg → 11500 g
    ((SELECT id FROM suppliers WHERE name = 'Transgourmet'),
     (SELECT id FROM inventory_items WHERE name = 'Heinz Tomato Ketchup'),
     'TG-3758877', 27.99, 11500.0, TRUE),

    -- Mayo: ST = 5 l → 5000 ml
    ((SELECT id FROM suppliers WHERE name = 'Transgourmet'),
     (SELECT id FROM inventory_items WHERE name = 'Hellmanns Mayonnaise'),
     'TG-3665023', 23.95,  5000.0, TRUE),

    -- Dijon Senf: ST = 5 kg → 5000 g
    ((SELECT id FROM suppliers WHERE name = 'Transgourmet'),
     (SELECT id FROM inventory_items WHERE name = 'Dijon Senf'),
     'TG-489062',  23.19,  5000.0, TRUE),

    -- French's Mustard: ST = 2,98 kg → 2980 g
    ((SELECT id FROM suppliers WHERE name = 'Transgourmet'),
     (SELECT id FROM inventory_items WHERE name = 'Frenchs Yellow Mustard'),
     'TG-3461688', 21.00,  2980.0, TRUE),

    -- Kikkoman: ST = 1,9 l → 1900 ml
    ((SELECT id FROM suppliers WHERE name = 'Transgourmet'),
     (SELECT id FROM inventory_items WHERE name = 'Kikkoman Soja Sauce'),
     'TG-425033',  13.99,  1900.0, TRUE),

    -- BBQ Sauce: ST = 1,814 kg → 1814 g
    ((SELECT id FROM suppliers WHERE name = 'Transgourmet'),
     (SELECT id FROM inventory_items WHERE name = 'Mississippi BBQ Sauce Sweet'),
     'TG-3591716',  9.59,  1814.0, TRUE),

    -- Transgourmet: Grundzutaten
    -- Kaffee: KT = 12 PK à 500 g → 6000 g
    ((SELECT id FROM suppliers WHERE name = 'Transgourmet'),
     (SELECT id FROM inventory_items WHERE name = 'Dallmayr Prodomo Kaffee'),
     'TG-143024',  14.99,  6000.0, TRUE),

    -- Zucker: PK = 10 kg → 10000 g
    ((SELECT id FROM suppliers WHERE name = 'Transgourmet'),
     (SELECT id FROM inventory_items WHERE name = 'Wiener Zucker Feinkristall'),
     'TG-3510609', 19.90, 10000.0, TRUE),

    -- Sonnenblumenöl: ST = 10 l → 10000 ml
    ((SELECT id FROM suppliers WHERE name = 'Transgourmet'),
     (SELECT id FROM inventory_items WHERE name = 'Economy Sonnenblumenöl'),
     'TG-946004',  25.69, 10000.0, TRUE),

    -- Salz: PK = 10 kg → 10000 g
    ((SELECT id FROM suppliers WHERE name = 'Transgourmet'),
     (SELECT id FROM inventory_items WHERE name = 'Bad Ischler Tafelsalz'),
     'TG-3289550',  8.69, 10000.0, TRUE),

    -- Pfeffer: ST = 7,9 l Streudose ≈ 3950 g (Schüttdichte ~0,5 kg/l)
    ((SELECT id FROM suppliers WHERE name = 'Transgourmet'),
     (SELECT id FROM inventory_items WHERE name = 'Wiberg Pfeffer schwarz ganz'),
     'TG-175745',  78.09,  3950.0, TRUE),

    -- Transgourmet: Käse & Eier
    -- Cheddar: KT = 6 PK à 1 kg → 6000 g
    ((SELECT id FROM suppliers WHERE name = 'Transgourmet'),
     (SELECT id FROM inventory_items WHERE name = 'Flander Cheddar Scheiben'),
     'TG-3855491', 10.55,  6000.0, TRUE),

    -- Eier: KT = 8 PK à 30 St → 240 Stück
    ((SELECT id FROM suppliers WHERE name = 'Transgourmet'),
     (SELECT id FROM inventory_items WHERE name = 'Bodenhaltungseier Gr L'),
     'TG-1311083', 14.96,   240.0, TRUE),

    -- Joseph Brot (Stückverkauf, 1 Stück pro Bestelleinheit)
    ((SELECT id FROM suppliers WHERE name = 'Joseph Brot'),
     (SELECT id FROM inventory_items WHERE name = 'Bio Joseph Brot Wecken 2kg TK'),
     'JB-WECKEN-2KG-TK',  18.90,  1.0, TRUE),

    ((SELECT id FROM suppliers WHERE name = 'Joseph Brot'),
     (SELECT id FROM inventory_items WHERE name = 'Bio Waldviertler Erdäpfelbrot TK'),
     'JB-ERDAEPFEL-TK',   12.50,  1.0, TRUE),

    ((SELECT id FROM suppliers WHERE name = 'Joseph Brot'),
     (SELECT id FROM inventory_items WHERE name = 'Bio La Marianne TK'),
     'JB-LAMARIANNE-TK',  15.90,  1.0, TRUE),

    -- Fleischhandel Meat
    -- Faschiertes: 1 kg Vakuumpackung → 1000 g
    ((SELECT id FROM suppliers WHERE name = 'Fleischhandel Meat'),
     (SELECT id FROM inventory_items WHERE name = 'Faschiertes'),
     'FM-FASCHIERTES-1KG', 8.90, 1000.0, TRUE);

-- ------------------------------------------------------------
-- 5. Sales Products (POS)
-- ------------------------------------------------------------
INSERT INTO sales_products (external_id, name, pos_system) VALUES
    ('BURGER-CLASSIC-001', 'Classic Burger', 'TestPOS'),
    ('BURGER-BBQ-001',     'BBQ Burger',     'TestPOS'),
    ('POMMES-001',         'Pommes',         'TestPOS'),
    ('SALAT-001',          'Beilagensalat',  'TestPOS');

-- Direkte POS → Inventory Mappings (Abzug pro Verkauf)
INSERT INTO pos_product_mappings (sales_product_id, inventory_item_id, qty_per_sale) VALUES
    ((SELECT id FROM sales_products WHERE external_id = 'POMMES-001'),
     (SELECT id FROM inventory_items WHERE name = 'Aviko Pommes Super Crunch 7mm'), 0.2),
    ((SELECT id FROM sales_products WHERE external_id = 'SALAT-001'),
     (SELECT id FROM inventory_items WHERE name = 'Eisbergsalat'), 0.5);

-- Rezeptur-Komponenten
INSERT INTO product_components (sales_product_id, inventory_item_id, qty_required) VALUES

    -- Classic Burger
    ((SELECT id FROM sales_products WHERE external_id = 'BURGER-CLASSIC-001'),
     (SELECT id FROM inventory_items WHERE name = 'Martins Potato Rolls 4 Inch'),    1.0),
    ((SELECT id FROM sales_products WHERE external_id = 'BURGER-CLASSIC-001'),
     (SELECT id FROM inventory_items WHERE name = 'Faschiertes'),                  180.0),
    ((SELECT id FROM sales_products WHERE external_id = 'BURGER-CLASSIC-001'),
     (SELECT id FROM inventory_items WHERE name = 'Flander Cheddar Scheiben'),      40.0),
    ((SELECT id FROM sales_products WHERE external_id = 'BURGER-CLASSIC-001'),
     (SELECT id FROM inventory_items WHERE name = 'Zwiebel weiss'),                  0.05),
    ((SELECT id FROM sales_products WHERE external_id = 'BURGER-CLASSIC-001'),
     (SELECT id FROM inventory_items WHERE name = 'Gurken'),                          0.3),
    ((SELECT id FROM sales_products WHERE external_id = 'BURGER-CLASSIC-001'),
     (SELECT id FROM inventory_items WHERE name = 'Eisbergsalat'),                    0.1),
    ((SELECT id FROM sales_products WHERE external_id = 'BURGER-CLASSIC-001'),
     (SELECT id FROM inventory_items WHERE name = 'Heinz Tomato Ketchup'),           20.0),
    ((SELECT id FROM sales_products WHERE external_id = 'BURGER-CLASSIC-001'),
     (SELECT id FROM inventory_items WHERE name = 'Hellmanns Mayonnaise'),           15.0),

    -- BBQ Burger
    ((SELECT id FROM sales_products WHERE external_id = 'BURGER-BBQ-001'),
     (SELECT id FROM inventory_items WHERE name = 'Martins Potato Rolls 4 Inch'),    1.0),
    ((SELECT id FROM sales_products WHERE external_id = 'BURGER-BBQ-001'),
     (SELECT id FROM inventory_items WHERE name = 'Faschiertes'),                  180.0),
    ((SELECT id FROM sales_products WHERE external_id = 'BURGER-BBQ-001'),
     (SELECT id FROM inventory_items WHERE name = 'Flander Cheddar Scheiben'),      40.0),
    ((SELECT id FROM sales_products WHERE external_id = 'BURGER-BBQ-001'),
     (SELECT id FROM inventory_items WHERE name = 'Mississippi BBQ Sauce Sweet'),   25.0),
    ((SELECT id FROM sales_products WHERE external_id = 'BURGER-BBQ-001'),
     (SELECT id FROM inventory_items WHERE name = 'Jalapeños'),                      0.02),
    ((SELECT id FROM sales_products WHERE external_id = 'BURGER-BBQ-001'),
     (SELECT id FROM inventory_items WHERE name = 'Eisbergsalat'),                   0.1),
    ((SELECT id FROM sales_products WHERE external_id = 'BURGER-BBQ-001'),
     (SELECT id FROM inventory_items WHERE name = 'Gurken'),                          0.3);

-- ------------------------------------------------------------
-- 6. Connector Configs
-- Pro Supplier genau 1 aktiver Connector (UNIQUE-Constraint)
-- ------------------------------------------------------------
INSERT INTO connector_configs (supplier_id, connector_type_id, config_payload, is_active) VALUES

    -- Transgourmet → EMAIL
    ((SELECT id FROM suppliers WHERE name = 'Transgourmet'),
     (SELECT id FROM connector_types WHERE name = 'EMAIL'),
     '{"recipientEmail":"wi23b139@technikum-wien.at","subjectTemplate":"Bestellung – Transgourmet – {date}","bodyTemplate":"Sehr geehrte Damen und Herren,\n\nbitte liefern Sie folgende Waren:\n\n{orderLines}\n\nMit freundlichen Grüßen,\nEl Reno OG"}',
     TRUE),

    -- Joseph Brot → EMAIL (Vorlage 1:1 aus Bestellungs-Screenshot)
    ((SELECT id FROM suppliers WHERE name = 'Joseph Brot'),
     (SELECT id FROM connector_types WHERE name = 'EMAIL'),
     '{"recipientEmail":"wi23b139@technikum-wien.at","subjectTemplate":"Bestellung / Joseph Brot","bodyTemplate":"Hallo Zusammen,\n\nWir brauchen für morgen früh bitte folgendes:\n\n{orderLines}\n\nDanke im Voraus und liebe Grüße,\nEl Reno OG"}',
     TRUE),

    -- Fleischhandel Meat → WHATSAPP
    ((SELECT id FROM suppliers WHERE name = 'Fleischhandel Meat'),
     (SELECT id FROM connector_types WHERE name = 'WHATSAPP'),
     '{"recipientPhone":"00491776284018","messageTemplate":"Bestellung {date} – Fleischhandel Meat:\n\n{orderLines}"}',
     TRUE);

-- ------------------------------------------------------------
-- 7. Initial Inventory Transactions (Anfangsbestand)
-- ★ = Low Stock
-- ------------------------------------------------------------
INSERT INTO inventory_transactions (inventory_item_id, transaction_type, delta, reference_id) VALUES
    ((SELECT id FROM inventory_items WHERE name = 'Zwiebel weiss'),                  'GOODS_RECEIPT',   10.0,   'INIT-ZWIEBEL'),
    ((SELECT id FROM inventory_items WHERE name = 'Eisbergsalat'),                   'GOODS_RECEIPT',   20.0,   'INIT-EISBERGSALAT'),
    ((SELECT id FROM inventory_items WHERE name = 'Gurken'),                         'GOODS_RECEIPT',   24.0,   'INIT-GURKEN'),
    ((SELECT id FROM inventory_items WHERE name = 'Jalapeños'),                      'GOODS_RECEIPT',    0.5,   'INIT-JALAPENOS'),       -- ★
    ((SELECT id FROM inventory_items WHERE name = 'Knoblauch'),                      'GOODS_RECEIPT',  100.0,   'INIT-KNOBLAUCH'),       -- ★
    ((SELECT id FROM inventory_items WHERE name = 'Aviko Pommes Super Crunch 7mm'),  'GOODS_RECEIPT',   10.0,   'INIT-POMMES'),
    ((SELECT id FROM inventory_items WHERE name = 'Martins Potato Rolls 4 Inch'),    'GOODS_RECEIPT',   20.0,   'INIT-POTATO-ROLLS'),    -- ★
    ((SELECT id FROM inventory_items WHERE name = 'Heinz Tomato Ketchup'),           'GOODS_RECEIPT', 11500.0,  'INIT-KETCHUP'),
    ((SELECT id FROM inventory_items WHERE name = 'Hellmanns Mayonnaise'),           'GOODS_RECEIPT',  5000.0,  'INIT-MAYO'),
    ((SELECT id FROM inventory_items WHERE name = 'Dijon Senf'),                     'GOODS_RECEIPT',  1000.0,  'INIT-DIJON-SENF'),      -- ★
    ((SELECT id FROM inventory_items WHERE name = 'Frenchs Yellow Mustard'),         'GOODS_RECEIPT',   500.0,  'INIT-YELLOW-MUSTARD'),  -- ★
    ((SELECT id FROM inventory_items WHERE name = 'Kikkoman Soja Sauce'),            'GOODS_RECEIPT',   400.0,  'INIT-SOJA-SAUCE'),      -- ★
    ((SELECT id FROM inventory_items WHERE name = 'Mississippi BBQ Sauce Sweet'),    'GOODS_RECEIPT',  1814.0,  'INIT-BBQ-SAUCE'),
    ((SELECT id FROM inventory_items WHERE name = 'Dallmayr Prodomo Kaffee'),        'GOODS_RECEIPT',   200.0,  'INIT-KAFFEE'),          -- ★
    ((SELECT id FROM inventory_items WHERE name = 'Wiener Zucker Feinkristall'),     'GOODS_RECEIPT', 10000.0,  'INIT-ZUCKER'),
    ((SELECT id FROM inventory_items WHERE name = 'Economy Sonnenblumenöl'),         'GOODS_RECEIPT', 10000.0,  'INIT-SONNENBLUMENOEL'),
    ((SELECT id FROM inventory_items WHERE name = 'Bad Ischler Tafelsalz'),          'GOODS_RECEIPT', 10000.0,  'INIT-SALZ'),
    ((SELECT id FROM inventory_items WHERE name = 'Wiberg Pfeffer schwarz ganz'),    'GOODS_RECEIPT',    80.0,  'INIT-PFEFFER'),         -- ★
    ((SELECT id FROM inventory_items WHERE name = 'Flander Cheddar Scheiben'),       'GOODS_RECEIPT',   500.0,  'INIT-CHEDDAR'),         -- ★
    ((SELECT id FROM inventory_items WHERE name = 'Bodenhaltungseier Gr L'),         'GOODS_RECEIPT',    60.0,  'INIT-EIER'),
    ((SELECT id FROM inventory_items WHERE name = 'Bio Joseph Brot Wecken 2kg TK'),    'GOODS_RECEIPT',  1.0,  'INIT-JB-WECKEN'),       -- ★
    ((SELECT id FROM inventory_items WHERE name = 'Bio Waldviertler Erdäpfelbrot TK'), 'GOODS_RECEIPT', 10.0,  'INIT-JB-ERDAEPFEL'),
    ((SELECT id FROM inventory_items WHERE name = 'Bio La Marianne TK'),               'GOODS_RECEIPT',  2.0,  'INIT-JB-LAMARIANNE'),   -- ★
    ((SELECT id FROM inventory_items WHERE name = 'Faschiertes'),                    'GOODS_RECEIPT',   800.0,  'INIT-FASCHIERTES');     -- ★

-- ------------------------------------------------------------
-- 8. Ready2Order Inbound Connector
-- ------------------------------------------------------------

-- Additional connector types for ready2order
INSERT INTO connector_types (name, direction, description) VALUES
    ('READY2ORDER_POLLING', 'INBOUND', 'Poll ready2order REST API for invoices'),
    ('READY2ORDER_WEBHOOK', 'INBOUND', 'Receive ready2order webhook events')
ON CONFLICT (name) DO NOTHING;

-- Ready2order app settings (tokens filled in via UI or env)
INSERT INTO app_settings (setting_key, setting_value) VALUES
    ('r2o.accountToken',           ''),
    ('r2o.developerToken',         ''),
    ('r2o.pollingIntervalMinutes', '5'),
    ('r2o.posSource',              'READY2ORDER'),
    ('r2o.lastPolledAt',           ''),
    ('r2o.enabled',                'false'),
    ('r2o.inboundMode',            'polling')
ON CONFLICT (setting_key) DO NOTHING;

-- Ready2order sales products (external_id = ready2order product_id)
INSERT INTO sales_products (external_id, name, pos_system) VALUES
    ('10001', 'Cola 0.33L',   'READY2ORDER'),
    ('10002', 'Cola 0.50L',   'READY2ORDER'),
    ('10003', 'Water 0.50L',  'READY2ORDER'),
    ('10004', 'Beer 0.50L',   'READY2ORDER'),
    ('10005', 'Beer 0.33L',   'READY2ORDER'),
    ('10006', 'Wine 0.75L',   'READY2ORDER'),
    ('10007', 'Cheeseburger', 'READY2ORDER'),
    ('10008', 'Hamburger',    'READY2ORDER')
ON CONFLICT (external_id) DO NOTHING;

-- Product components: burgers mapped to existing inventory items
-- Drinks (10001–10006) have no tracked inventory items → no components
INSERT INTO product_components (sales_product_id, inventory_item_id, qty_required) VALUES
    -- Cheeseburger
    ((SELECT id FROM sales_products WHERE external_id = '10007'),
     (SELECT id FROM inventory_items WHERE name = 'Martins Potato Rolls 4 Inch'),  1.0),
    ((SELECT id FROM sales_products WHERE external_id = '10007'),
     (SELECT id FROM inventory_items WHERE name = 'Faschiertes'),                150.0),
    ((SELECT id FROM sales_products WHERE external_id = '10007'),
     (SELECT id FROM inventory_items WHERE name = 'Flander Cheddar Scheiben'),    30.0),

    -- Hamburger
    ((SELECT id FROM sales_products WHERE external_id = '10008'),
     (SELECT id FROM inventory_items WHERE name = 'Martins Potato Rolls 4 Inch'),  1.0),
    ((SELECT id FROM sales_products WHERE external_id = '10008'),
     (SELECT id FROM inventory_items WHERE name = 'Faschiertes'),                150.0);
