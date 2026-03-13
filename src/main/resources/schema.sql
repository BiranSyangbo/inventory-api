-- Liquor Shop Inventory & Billing System
-- PostgreSQL Schema
-- Valuation: Weighted Average Cost

-- ─────────────────────────────────────────
-- AUTH
-- ─────────────────────────────────────────

CREATE TABLE IF NOT EXISTS users (
  id          BIGSERIAL PRIMARY KEY,
  username    VARCHAR(50)  UNIQUE NOT NULL,
  email       VARCHAR(255) UNIQUE,
  password    VARCHAR(255) NOT NULL,
  enabled     BOOLEAN      NOT NULL DEFAULT true,
  created_at  TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_users_username ON users(username);
CREATE INDEX IF NOT EXISTS idx_users_email    ON users(email);

CREATE TABLE IF NOT EXISTS refresh_tokens (
  id         BIGSERIAL PRIMARY KEY,
  token      VARCHAR(255) UNIQUE NOT NULL,
  user_id    BIGINT       NOT NULL REFERENCES users(id) ON DELETE CASCADE,
  expiry_at  TIMESTAMP    NOT NULL,
  created_at TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_refresh_tokens_token     ON refresh_tokens(token);
CREATE INDEX IF NOT EXISTS idx_refresh_tokens_user_id   ON refresh_tokens(user_id);
CREATE INDEX IF NOT EXISTS idx_refresh_tokens_expiry_at ON refresh_tokens(expiry_at);

-- ─────────────────────────────────────────
-- MASTER DATA
-- ─────────────────────────────────────────

CREATE TABLE IF NOT EXISTS suppliers (
  id             BIGSERIAL PRIMARY KEY,
  name           VARCHAR(255) NOT NULL,
  contact_person VARCHAR(255),
  phone          VARCHAR(50),
  address        TEXT,
  vat_pan_number VARCHAR(50),
  status         VARCHAR(20)  NOT NULL DEFAULT 'ACTIVE', -- ACTIVE | INACTIVE
  created_at     TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_suppliers_status ON suppliers(status);

CREATE TABLE IF NOT EXISTS products (
  id            BIGSERIAL PRIMARY KEY,
  name          VARCHAR(255)   NOT NULL,
  brand         VARCHAR(255),
  category      VARCHAR(100),
  volume_ml     VARCHAR(50),
  `type`          VARCHAR(50),
    percentage    NUMBER(5,2),
    mrp       VARCHAR(255)   UNIQUE,
    current_stock     INTEGER        NOT NULL DEFAULT 0,
    barcode       VARCHAR(255)   UNIQUE,
  min_stock     INTEGER        NOT NULL DEFAULT 0,
  selling_price DECIMAL(10,2)  NOT NULL DEFAULT 0,
  -- Weighted average cost; recalculated on every purchase line
  average_cost  DECIMAL(10,2)  NOT NULL DEFAULT 0,
  status        VARCHAR(20)    NOT NULL DEFAULT 'ACTIVE', -- ACTIVE | INACTIVE
  deleted       BOOLEAN        NOT NULL DEFAULT false,    -- soft-delete; hidden from frontend
  created_at    TIMESTAMP      NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_products_barcode  ON products(barcode);
CREATE INDEX IF NOT EXISTS idx_products_category ON products(category);
CREATE INDEX IF NOT EXISTS idx_products_status   ON products(status);

-- ─────────────────────────────────────────
-- CUSTOMERS (credit shops only)
-- ─────────────────────────────────────────

CREATE TABLE IF NOT EXISTS customers (
  id                  BIGSERIAL PRIMARY KEY,
  name                VARCHAR(255)  NOT NULL,
  phone               VARCHAR(50),
  address             TEXT,
  credit_limit        DECIMAL(10,2) NOT NULL DEFAULT 0,
  outstanding_balance DECIMAL(10,2) NOT NULL DEFAULT 0, -- updated on sale / payment
  created_at          TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Per-product custom selling price per credit customer
CREATE TABLE IF NOT EXISTS customer_price_templates (
  id            BIGSERIAL PRIMARY KEY,
  customer_id   BIGINT        NOT NULL REFERENCES customers(id) ON DELETE CASCADE,
  product_id    BIGINT        NOT NULL REFERENCES products(id)  ON DELETE CASCADE,
  selling_price DECIMAL(10,2) NOT NULL,
  UNIQUE(customer_id, product_id)
);

CREATE INDEX IF NOT EXISTS idx_cpt_customer_id ON customer_price_templates(customer_id);
CREATE INDEX IF NOT EXISTS idx_cpt_product_id  ON customer_price_templates(product_id);

-- ─────────────────────────────────────────
-- BATCHES  (merged BatchEntity + ProductBatchEntity)
-- Used for: expiry tracking + stock quantity tracking
-- Cost for profit = product.average_cost (NOT batch.purchase_price)
-- ─────────────────────────────────────────

CREATE TABLE IF NOT EXISTS batches (
  id                BIGSERIAL PRIMARY KEY,
  product_id        BIGINT        NOT NULL REFERENCES products(id) ON DELETE CASCADE,
  batch_code        VARCHAR(255),               -- auto-generated on purchase
  purchase_date     DATE          NOT NULL DEFAULT CURRENT_DATE,
  expiry_date       DATE,                       -- nullable
  purchase_price    DECIMAL(10,2) NOT NULL,     -- cost per unit at time of purchase (reference)
  original_quantity INTEGER       NOT NULL,
  current_quantity  INTEGER       NOT NULL,
  location          VARCHAR(255),
  created_at        TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_batches_product_id   ON batches(product_id);
CREATE INDEX IF NOT EXISTS idx_batches_batch_code   ON batches(batch_code);
CREATE INDEX IF NOT EXISTS idx_batches_expiry_date  ON batches(expiry_date);

-- ─────────────────────────────────────────
-- PURCHASES (Stock IN)
-- ─────────────────────────────────────────

CREATE TABLE IF NOT EXISTS purchases (
  id              BIGSERIAL PRIMARY KEY,
  supplier_id     BIGINT        NOT NULL REFERENCES suppliers(id),
  vat_bill_number VARCHAR(255)  UNIQUE,          -- nullable; unique when provided
  purchase_date   DATE          NOT NULL DEFAULT CURRENT_DATE,
  invoice_amount  DECIMAL(10,2),                 -- total from distributor bill
  vat_amount      DECIMAL(10,2) DEFAULT 0,       -- VAT from distributor bill (not calculated)
  discount        DECIMAL(10,2) DEFAULT 0,
  remarks         TEXT,
  created_at      TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_purchases_supplier_id     ON purchases(supplier_id);
CREATE INDEX IF NOT EXISTS idx_purchases_purchase_date   ON purchases(purchase_date);
CREATE INDEX IF NOT EXISTS idx_purchases_vat_bill_number ON purchases(vat_bill_number);

CREATE TABLE IF NOT EXISTS purchase_lines (
  id             BIGSERIAL PRIMARY KEY,
  purchase_id    BIGINT        NOT NULL REFERENCES purchases(id) ON DELETE CASCADE,
  product_id     BIGINT        NOT NULL REFERENCES products(id),
  batch_id       BIGINT        NOT NULL REFERENCES batches(id),
  quantity       INTEGER       NOT NULL,
  purchase_price DECIMAL(10,2) NOT NULL, -- per unit; also triggers avg_cost recalculation
  vat_percent    DECIMAL(5,2)  DEFAULT 0
);

CREATE INDEX IF NOT EXISTS idx_purchase_lines_purchase_id ON purchase_lines(purchase_id);
CREATE INDEX IF NOT EXISTS idx_purchase_lines_product_id  ON purchase_lines(product_id);
CREATE INDEX IF NOT EXISTS idx_purchase_lines_batch_id    ON purchase_lines(batch_id);

-- Payments made TO suppliers (multiple per purchase allowed)
CREATE TABLE IF NOT EXISTS purchase_payments (
  id               BIGSERIAL PRIMARY KEY,
  purchase_id      BIGINT        NOT NULL REFERENCES purchases(id) ON DELETE CASCADE,
  supplier_id      BIGINT        NOT NULL REFERENCES suppliers(id),
  payment_date     TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  amount           DECIMAL(10,2) NOT NULL,
  payment_method   VARCHAR(20)   NOT NULL, -- CASH | ONLINE | CHEQUE
  reference_number VARCHAR(255),           -- cheque no. or transaction ID
  notes            TEXT
);

CREATE INDEX IF NOT EXISTS idx_purchase_payments_purchase_id ON purchase_payments(purchase_id);
CREATE INDEX IF NOT EXISTS idx_purchase_payments_supplier_id ON purchase_payments(supplier_id);

-- ─────────────────────────────────────────
-- SALES (Stock OUT)
-- ─────────────────────────────────────────

CREATE TABLE IF NOT EXISTS sales (
  id             BIGSERIAL PRIMARY KEY,
  customer_id    BIGINT        REFERENCES customers(id), -- nullable for walk-in
  invoice_number VARCHAR(50)   UNIQUE NOT NULL,          -- auto-generated: INV-YYYY-NNNNN
  sale_date      TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  total_amount   DECIMAL(10,2) NOT NULL DEFAULT 0,
  discount       DECIMAL(10,2) DEFAULT 0,
  vat_amount     DECIMAL(10,2) DEFAULT 0,
  payment_status VARCHAR(20)   NOT NULL DEFAULT 'PAID',  -- PAID | PARTIAL | CREDIT
  notes          TEXT,
  created_at     TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_sales_customer_id    ON sales(customer_id);
CREATE INDEX IF NOT EXISTS idx_sales_invoice_number ON sales(invoice_number);
CREATE INDEX IF NOT EXISTS idx_sales_sale_date      ON sales(sale_date);
CREATE INDEX IF NOT EXISTS idx_sales_payment_status ON sales(payment_status);

CREATE TABLE IF NOT EXISTS sale_lines (
  id                 BIGSERIAL PRIMARY KEY,
  sale_id            BIGINT        NOT NULL REFERENCES sales(id) ON DELETE CASCADE,
  batch_id           BIGINT        NOT NULL REFERENCES batches(id),
  product_id         BIGINT        NOT NULL REFERENCES products(id), -- denormalised for queries
  quantity           INTEGER       NOT NULL,
  unit_price         DECIMAL(10,2) NOT NULL, -- from customer template or product.selling_price
  cost_price_at_sale DECIMAL(10,2) NOT NULL, -- snapshot of product.average_cost at time of sale
  line_total         DECIMAL(10,2) NOT NULL  -- quantity × unit_price
);

CREATE INDEX IF NOT EXISTS idx_sale_lines_sale_id    ON sale_lines(sale_id);
CREATE INDEX IF NOT EXISTS idx_sale_lines_batch_id   ON sale_lines(batch_id);
CREATE INDEX IF NOT EXISTS idx_sale_lines_product_id ON sale_lines(product_id);

-- Payments received FROM customers
CREATE TABLE IF NOT EXISTS sale_payments (
  id               BIGSERIAL PRIMARY KEY,
  customer_id      BIGINT        NOT NULL REFERENCES customers(id),
  sale_id          BIGINT        REFERENCES sales(id), -- nullable: can be a general payment
  payment_date     TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  amount           DECIMAL(10,2) NOT NULL,
  payment_method   VARCHAR(20)   NOT NULL, -- CASH | ONLINE | CHEQUE
  reference_number VARCHAR(255),           -- cheque no. or transaction ID
  notes            TEXT
);

CREATE INDEX IF NOT EXISTS idx_sale_payments_customer_id ON sale_payments(customer_id);
CREATE INDEX IF NOT EXISTS idx_sale_payments_sale_id     ON sale_payments(sale_id);
