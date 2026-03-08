# Stock Management System
## Liquor Shop Inventory & Billing System

---

# 1. PROJECT OVERVIEW

## 1.1 Purpose
Develop a complete stock and billing management system for a liquor retail shop to:
- Track real-time inventory
- Manage purchase with VAT bills
- Track sales and profit
- Generate reports
- Prevent stock mismatch

## 1.2 Target Users
- Owner

---

# 2. SYSTEM MODULES

---

# 3. MASTER DATA MANAGEMENT

## 3.1 Product Master

Each product (SKU) must contain:

- Product Name
- Brand Name
- Category (Whiskey, Vodka, Beer, Wine, Rum, etc.)
- Volume (180ml / 375ml / 750ml / 1L)
- Minimum Stock Level
- Selling Price
- barcode
- Status (Active / Inactive)
- Current Stock
- MRP

> Note: Same brand with different volume must be treated as separate SKU.
---

## 3.2 Supplier Master

- Supplier Name
- VAT/PAN Number (Optional)
- Contact Person
- Phone Number
- Address
- Status

---

## 3.3 Tax Master

- VAT Percentage
- Excise Duty (if applicable)

---

# 4. PURCHASE MANAGEMENT (Stock IN)

## 4.1 Purchase Invoice Entry

Fields:
- Supplier(Should be dropdown where show the above suppliers)
- VAT Bill Number (Unique)(Optional)
- Purchase Date
- Invoice Amount
- VAT Amount
- Discount
- Payment Type (Cash / Credit/ Online) 
- Remarks

#### rule for payment type

- Create separate payment table
- Allow multiple payment entries 
- Auto calculate balance 
- Track payment history

## 4.2 Purchase Item Details

For each product:
- Suppliers 
- Product Name
- Quantity
- Purchase Price Per Unit
- VAT % per item
- Batch Number (Optional)(Auto generated)
- Expiry Date (Optional)

---

## 4.3 Rules

- VAT Bill Number must be unique.
- Same product can have different purchase prices.
- Maintain purchase history.
- Stock should increase automatically after purchase save.
- Duplicate bill entry must be rejected.
- Can Compare every product brought pricing as a history

---

# 5. SALES MANAGEMENT (Stock OUT)

## 5.1 Sales Invoice

- Invoice Number (Auto-generated)
- Invoice Date
- Customer Type (Walk-in / Wholesale)
- Customer Name (Optional)
- Payment Type (Cash / Online / Credit)
- Discount
- VAT

## 5.2 Sales Item Details

- Product Name
- Quantity
- Selling Price
- Discount
- VAT
- Line Total

---

## 5.3 Sales Rules

- Cannot sell if stock quantity is insufficient.
- Stock should decrease automatically after sale.
- Profit should be calculated:
  Profit = Selling Price - Cost Price
- Invoice number must be auto-generated and unique.

---

# 6. STOCK CONTROL MODULE

## 6.1 Real-Time Stock

System must provide:
- Current Stock Quantity
- Stock Value
- Stock by Product
- Stock by Category

---

## 6.2 Stock Ledger

Maintain complete stock movement history:

- Date
- Transaction Type (Purchase / Sale / Adjustment)
- Reference Number
- Quantity IN
- Quantity OUT
- Balance Quantity

---

## 6.3 Stock Adjustment

Allow manual stock correction:

- Adjustment Type (Damage / Breakage / Theft / Correction)
- Product
- Quantity
- Reason
- Approval (Optional)

---

# 7. REPORTING MODULE

System must generate:

- Daily Sales Report
- Monthly Sales Report
- Purchase Report
- VAT Report
- Stock Summary Report
- Stock Movement Report
- Low Stock Report
- Fast Moving Products Report
- Dead Stock Report
- Profit & Loss Summary
- Supplier Outstanding Report

---

# 8. ACCOUNTING FEATURES (Basic)

- Credit Purchase Tracking
- Credit Sales Tracking
- Supplier Payable Report
- Customer Receivable Report
- Expense Entry
- Cashbook Summary

---

# 9. USER ROLE MANAGEMENT

## 9.1 Owner
- Full Access


---

# 10. VALIDATION RULES

- Stock cannot go negative.
- VAT Bill number must be unique.
- Selling price cannot be zero.
- Required fields must not be empty.
- Duplicate product SKU must not be allowed.
- Expiry alert notifications


---

# 11. NON-FUNCTIONAL REQUIREMENTS

- Response time under 2 seconds
- Secure login system
- Audit log (track edits and deletes)
- Data backup and restore feature
- Scalable up to 50,000+ records
- Fast barcode search support

---

---

# 13. OPEN DECISIONS (RESOLVED)

| Decision | Resolution |
|----------|-----------|
| Stock valuation | Weighted Average Cost; batches kept for expiry tracking only |
| VAT on selling price | VAT added separately; `vat_amount` stored from distributor invoice |
| Credit sales | Yes — tracked via `sale_payments`, `customer.outstanding_balance` |
| Customer price template | Per-product per-customer; auto-filled on sale; walk-in excluded |
| Multi-branch | Single store only |
| Supplier | Proper master table, dropdown on purchase form |
| VAT bill uniqueness | Enforced at DB level (UNIQUE, NULLs allowed) |
| Product status | `status` field (ACTIVE/INACTIVE); `deleted=true` = hidden from frontend |
| Batch entities | Merged `BatchEntity` + `ProductBatchEntity` into single `batches` table |

---

# 14. IMPLEMENTATION PLAN

---

## BACKEND — Spring Boot API

### Phase 1: Database Schema  (`schema.sql`)
- [x] `users`, `refresh_tokens` (unchanged)
- [x] `suppliers`
- [x] `products` — add `selling_price`, `average_cost`, `status`
- [x] `customers` — `name`, `phone`, `address`, `credit_limit`, `outstanding_balance`
- [x] `customer_price_templates` — per-product price per customer
- [x] `batches` — merged; fields: `purchase_date`, `expiry_date`, `purchase_price`, `original_quantity`, `current_quantity`, `location`
- [x] `purchases` — FK supplier, `vat_bill_number` (UNIQUE nullable), `invoice_amount`, `vat_amount`, `discount`, `remarks`
- [x] `purchase_lines` — FK batch, `vat_percent`
- [x] `purchase_payments` — `payment_method` (CASH/ONLINE/CHEQUE), `reference_number`
- [x] `sales` — FK customer (nullable), `invoice_number` (auto-gen), `payment_status` (PAID/PARTIAL/CREDIT)
- [x] `sale_lines` — `unit_price`, `cost_price_at_sale` (snapshot of avg cost), `line_total`
- [x] `sale_payments` — FK customer + optional sale, `payment_method`, `reference_number`

### Phase 2: Entities
- [x] `SupplierEntity`
- [x] `ProductEntity` (update)
- [x] `CustomerEntity`
- [x] `CustomerPriceTemplateEntity`
- [x] `BatchEntity` (merged, update)
- [x] `PurchaseEntity` (update)
- [x] `PurchaseLineEntity` (update)
- [x] `PurchasePaymentEntity`
- [x] `SaleEntity` (update)
- [x] `SaleLineEntity` (update)
- [x] `SalePaymentEntity`

### Phase 3: Repositories
- [x] `SupplierRepository`
- [x] `CustomerRepository`
- [x] `CustomerPriceTemplateRepository`
- [x] Update: `BatchRepository`, `PurchaseRepository`, `PurchaseLineRepository`
- [x] `PurchasePaymentRepository`
- [x] Update: `SaleRepository`, `SaleLineRepository`
- [x] `SalePaymentRepository`

### Phase 4: Services + Business Logic

- [x] **SupplierService** — CRUD
- [x] **ProductService** — CRUD, status toggle, soft delete
- [x] **CustomerService** — CRUD, `outstanding_balance` updated on sale/payment, price template management, customer statement
- [x] **PurchaseService** — create purchase → create batches → recalculate `product.average_cost`, enforce unique `vat_bill_number`, payment recording
- [x] **SaleService** — auto-fill prices from template, expiry-first batch allocation, `cost_price_at_sale` snapshot, invoice number auto-gen, credit limit check, payment recording
- [x] **InventoryService** — current stock, low stock, expiring batches

### Phase 5: Controllers

| Method | Endpoint | Description |
|--------|----------|-------------|
| POST | `/api/auth/register` | Register |
| POST | `/api/auth/login` | Login |
| POST | `/api/auth/refresh` | Refresh token |
| POST | `/api/auth/logout` | Logout |
| GET | `/api/auth/me` | Current user |
| GET/POST | `/api/suppliers` | List / create |
| GET/PUT/DELETE | `/api/suppliers/{id}` | Get / update / delete |
| GET/POST | `/api/products` | List / create |
| GET/PUT/DELETE | `/api/products/{id}` | Get / update / soft-delete |
| GET/POST | `/api/customers` | List / create |
| GET/PUT/DELETE | `/api/customers/{id}` | Get / update / delete |
| GET/POST | `/api/customers/{id}/price-template` | Get / upsert template |
| DELETE | `/api/customers/{id}/price-template/{productId}` | Remove one entry |
| GET | `/api/customers/{id}/statement` | Statement (sales + payments, journal-ready) |
| POST | `/api/purchases` | Create purchase (creates batches, updates avg cost) |
| GET | `/api/purchases` | List purchases |
| GET | `/api/purchases/{id}` | Purchase detail |
| POST | `/api/purchases/{id}/payments` | Record supplier payment |
| GET | `/api/purchases/{id}/payments` | Payment history |
| POST | `/api/sales` | Create sale |
| GET | `/api/sales` | List sales |
| GET | `/api/sales/{id}` | Sale detail |
| POST | `/api/sales/{id}/payments` | Record customer payment |
| GET | `/api/customers/{id}/payments` | Customer payment history |
| GET | `/api/inventory` | Current stock all products |
| GET | `/api/inventory/low-stock` | Below min stock |
| GET | `/api/inventory/expiring?days=30` | Batches expiring within N days |

> Phase 5 complete — all controllers implemented and build passing.

---

## FRONTEND — React / Next.js (Separate Project)

### Screens by Module

**Auth**
- [ ] Login page

**Dashboard**
- [ ] Stock summary cards: total products, low stock count, expiring soon count
- [ ] Quick sale shortcut

**Suppliers**
- [ ] Supplier list (table, status filter)
- [ ] Add / Edit supplier form

**Products**
- [ ] Product list (filter: category, status, low stock, barcode search)
- [ ] Add / Edit product form
- [ ] Per-product batch history view (expiry dates, remaining qty, purchase prices)

**Customers**
- [ ] Customer list with outstanding balance
- [ ] Add / Edit customer form
- [ ] Price template editor (product → custom price per customer)
- [ ] Customer statement page (sales + payments in journal-entry style, printable)

**Purchases**
- [ ] Purchase list (filter: date range, supplier)
- [ ] Create purchase form
  - Supplier dropdown, VAT bill no., date, invoice amount, VAT amount, discount, remarks
  - Line items: product, qty, purchase price, VAT %, auto-generated batch code, expiry date
- [ ] Purchase detail + payment history
- [ ] Add payment modal (CASH / ONLINE / CHEQUE + reference number)

**Sales**
- [ ] Sale list (filter: date, payment status, customer)
- [ ] Create sale form
  - Optional customer dropdown → auto-fills prices from template
  - Line items: product search / barcode scan, qty, unit price
  - Payment status: PAID / PARTIAL / CREDIT
- [ ] Sale detail / invoice view (printable)
- [ ] Add payment modal for PARTIAL / CREDIT sales

**Inventory**
- [ ] Current stock table (product, total qty, avg cost, stock value, low stock flag)
- [ ] Low stock alert list
- [ ] Expiring batches list (colour-coded: expired / expiring soon)

**Reports** *(Phase 2)*
- [ ] Daily / Monthly sales report
- [ ] Purchase report
- [ ] VAT report
- [ ] Profit & Loss summary (uses `cost_price_at_sale` vs `unit_price`)
- [ ] Supplier outstanding report
- [ ] Stock movement report

---

# END OF REQUIREMENT DOCUMENT