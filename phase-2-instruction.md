You are a senior software architect working on a Liquor Shop Inventory & Billing System.

The project already has **Phase 1 implemented**, including:

- Product management
- Supplier management
- Customer management
- Purchase management
- Sales management
- Inventory tracking
- Stock ledger
- Payment tracking
- Weighted average cost calculation
- Credit tracking
- CRUD APIs
- Core backend services
- React frontend screens for CRUD

The detailed requirements are defined in:

@requirement.md

---

# Task

Now we want to implement **Phase 2: Reporting Module**.

The reporting business logic and definitions already exist in:

@documents/reporting-business.md

Your task is to:

1. **Study the existing system requirements**
2. **Analyze the reporting document**
3. **Exclude reports that are already covered by existing APIs or logic**
4. **Design the reporting layer in a way that integrates cleanly with the existing architecture**

---

# Important Constraints

The current system already supports:

- Purchases with multiple purchase prices
- Weighted Average Cost
- Sales with cost snapshot (`cost_price_at_sale`)
- Inventory batches
- Credit sales
- Supplier payments
- Customer payments
- Stock ledger

Reports must **reuse these existing tables and logic**.

Do NOT redesign the data model unless absolutely necessary.

---

# What You Need To Generate

## 1. Backend Reporting Design

Create a document describing:

### Report APIs

Define APIs such as:

GET /api/reports/daily-sales  
GET /api/reports/monthly-sales  
GET /api/reports/profit-loss  
GET /api/reports/purchase-report  
GET /api/reports/vat-report  
GET /api/reports/stock-summary  
GET /api/reports/stock-movement  
GET /api/reports/low-stock  
GET /api/reports/fast-moving-products  
GET /api/reports/dead-stock  
GET /api/reports/supplier-outstanding

For each API define:

- Purpose
- Query parameters
- Data sources (tables)
- Example response structure

---

## 2. Reporting Query Logic

Explain how each report should be calculated using existing tables such as:

- sales
- sale_lines
- purchases
- purchase_lines
- batches
- products
- suppliers
- customers
- sale_payments
- purchase_payments

Focus especially on:

- Profit calculation
- Stock movement
- Inventory valuation
- Credit tracking

---

## 3. Backend Architecture

Define:

- `ReportService`
- `ReportRepository` (if needed)
- `ReportController`

Explain how reports should be implemented efficiently.

For example:

- aggregation queries
- pagination
- date filters

---

## 4. Frontend Reporting Specification

Generate a **separate markdown file**:

frontend-reporting-spec.md

This document should ensure the frontend is **fully aligned with backend APIs**.

For each report define:

- API endpoint
- UI layout
- table columns
- filters (date range, supplier, product, category)
- charts (if useful)

Example:

Daily Sales Report

Columns:

Date  
Total Sales  
Total Profit  
Total VAT  
Invoice Count

Filters:

Date range

---

## 5. Dashboard Metrics

Define metrics for the owner dashboard:

- Today's Sales
- Today's Profit
- Low Stock Count
- Expiring Stock
- Total Stock Value
- Pending Customer Credit
- Pending Supplier Payments

Explain how each metric is calculated.

---

# Output Format

Generate the following files:

1️⃣ reporting-backend-design.md  
2️⃣ frontend-reporting-spec.md

Ensure both documents are **consistent with the current backend architecture and database schema** defined in @requirement.md.

---

# Goal

The goal is to create a **clean reporting layer** for the Liquor Shop Inventory System that supports:

- Business decision making
- Profit analysis
- Inventory optimization
- Credit monitoring


Do not generate generic POS reports.

All reports must be specifically designed for a **Liquor Shop Inventory System** with:

- VAT purchases
- Batch inventory
- Weighted average cost
- Credit sales
- Supplier payments