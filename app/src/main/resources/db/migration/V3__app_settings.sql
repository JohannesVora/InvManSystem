-- ============================================================
-- V3: App Settings Table
-- ============================================================

CREATE TABLE IF NOT EXISTS app_settings (
    id            INTEGER PRIMARY KEY AUTOINCREMENT,
    setting_key   TEXT NOT NULL UNIQUE,
    setting_value TEXT
);

-- Default SMTP settings (to be overridden via API)
INSERT OR IGNORE INTO app_settings (setting_key, setting_value) VALUES
    ('smtp.host',     'smtp.example.com'),
    ('smtp.port',     '587'),
    ('smtp.username', ''),
    ('smtp.password', ''),
    ('smtp.from',     '');
