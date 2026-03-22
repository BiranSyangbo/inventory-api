# Phase 2 — Reporting Backend Design

Liquor Shop Inventory & Billing System

---

## 1. Architecture Overview

The reporting layer is implemented as a **thin dedicated slice** that sits on top of the existing Phase 1 data model. No schema changes are required.

```
ReportController  →  ReportService  →  EntityManager (native SQL)
                                    →  existing tables
```

### Why `EntityManager` native queries?

Reporting queries are **read-only, multi-table aggregations** that do not map cleanly to a single JPA entity. Using `EntityManager.createNativeQuery()` gives:

- Full PostgreSQL SQL (CTEs, subqueries, UNION ALL, window functions)
- No risk of N+1 from lazy associations
- Clear separation — reporting never mutates domain state

All queries are wrapped in `@Transactional(readOnly = true)` which lets Hibernate skip dirty-checking and use a read-only JDBC connection.

---

## 2. Files Created

| Layer | File |
|-------|------|
| Controller | `controller/ReportController.java` |
| Service | `service/ReportService.java` |
| DTO | `dto/DailySalesRow.java` |
| DTO | `dto/ProfitLossRow.java` |
| DTO | `dto/PurchaseReportRow.java` |
| DTO | `dto/VatReportResponse.java` |
| DTO | `dto/VatPurchaseLine.java` |
| DTO | `dto/VatSalesLine.java` |
| DTO | `dto/StockMovementRow.java` |
| DTO | `dto/FastMovingProductRow.java` |
| DTO | `dto/DeadStockRow.java` |
| DTO | `dto/SupplierOutstandingRow.java` |
| DTO | `dto/CategorySalesRow.java` |
| DTO | `dto/DashboardResponse.java` |

---

## 3. Report API Reference

### Base path: `/api/reports`

All endpoints require `Authorization: Bearer <token>`.

---

### 3.1 `GET /api/reports/daily-sales`

**Purpose:** Day-by-day sales summary for a date range. Supports daily monitoring and trend spotting.

**Query parameters:**

| Param | Type | Default | Description |
|-------|------|---------|-------------|
| `from` | `date` (ISO-8601) | today − 30 days | Range start (inclusive) |
| `to`   | `date` (ISO-8601) | today | Range end (inclusive) |

**Data sources:** `sales`, `sale_lines`

**Key design note:** Sale-level aggregates (`total_amount`, `vat_amount`) and line-level profit aggregates are computed in **separate subqueries** then joined by date. This prevents double-counting when a sale has multiple line items.

**Example response:**
```json
[
  {
    "date": "2026-03-01",
    "invoiceCount": 95,
    "totalSales": 120000.00,
    "totalProfit": 22000.00,
    "totalVat": 1560.00,
    "walkInSales": 70000.00,
    "customerSales": 50000.00
  }
]
```

**Profit formula:**
```
totalProfit = SUM(sl.quantity * (sl.unit_price - sl.cost_price_at_sale))
```
`cost_price_at_sale` is the WAC snapshot stored at time of sale — never retroactively updated.

---

### 3.2 `GET /api/reports/profit-loss`

**Purpose:** Per-product P&L for a period. Identifies which products are most/least profitable.

**Query parameters:** `from`, `to` (same as above)

**Data sources:** `sale_lines`, `products`, `sales`

**Example response:**
```json
[
  {
    "productId": 12,
    "productName": "Jack Daniels",
    "brand": "Jack Daniels",
    "category": "Whiskey",
    "quantitySold": 15,
    "revenue": 148500.00,
    "totalCost": 130500.00,
    "profit": 18000.00,
    "marginPct": 12.12
  }
]
```

Ordered by `profit DESC`. `marginPct = profit / revenue × 100`.

---

### 3.3 `GET /api/reports/purchase-report`

**Purpose:** Purchase invoice history with per-invoice payment tracking.

**Query parameters:**

| Param | Type | Default | Description |
|-------|------|---------|-------------|
| `from` | `date` | today − 30 days | Purchase date range start |
| `to`   | `date` | today | Purchase date range end |
| `supplierId` | `long` | — | Optional supplier filter |

**Data sources:** `purchases`, `suppliers`, `purchase_payments`

**Calculation:**
```
outstanding = invoice_amount - SUM(purchase_payments.amount) for this purchase
```

**Example response:**
```json
[
  {
    "purchaseId": 5,
    "purchaseDate": "2026-02-15",
    "supplierName": "Himalayan Distributor",
    "vatBillNumber": "HIM-2026-0012",
    "invoiceAmount": 84000.00,
    "vatAmount": 10920.00,
    "discount": 0.00,
    "totalPaid": 60000.00,
    "outstanding": 24000.00
  }
]
```

---

### 3.4 `GET /api/reports/vat-report`

**Purpose:** VAT reconciliation — input VAT from purchases vs output VAT from sales, with net liability.

**Query parameters:** `from`, `to`

**Data sources:** `sales`, `purchases`, `suppliers`

**Only includes records where `vat_amount > 0`.**

**Example response:**
```json
{
  "totalPurchaseVat": 15600.00,
  "totalSalesVat": 23400.00,
  "netVatLiability": 7800.00,
  "purchaseLines": [
    {
      "purchaseId": 5,
      "purchaseDate": "2026-02-15",
      "supplierName": "Himalayan Distributor",
      "vatBillNumber": "HIM-2026-0012",
      "invoiceAmount": 84000.00,
      "vatAmount": 10920.00
    }
  ],
  "salesLines": [
    {
      "saleId": 101,
      "saleDate": "2026-02-16",
      "invoiceNumber": "INV-2026-00101",
      "totalAmount": 9800.00,
      "vatAmount": 1274.00
    }
  ]
}
```

`netVatLiability = totalSalesVat − totalPurchaseVat`

---

### 3.5 `GET /api/reports/stock-movement`

**Purpose:** Complete stock ledger for a single product — every purchase IN and sale OUT, with running balance.

**Query parameters:**

| Param | Type | Default | Description |
|-------|------|---------|-------------|
| `productId` | `long` | **required** | Product to trace |
| `from` | `date` | today − 90 days | Range start |
| `to`   | `date` | today | Range end |

**Data sources:** `purchase_lines`, `purchases`, `batches`, `sale_lines`, `sales`

Uses `UNION ALL` to merge purchase and sale movements, ordered by date. Running balance computed in Java.

**Example response:**
```json
[
  {
    "date": "2026-02-15",
    "transactionType": "PURCHASE",
    "referenceNumber": "HIM-2026-0012",
    "quantityIn": 100,
    "quantityOut": 0,
    "runningBalance": 100
  },
  {
    "date": "2026-02-16",
    "transactionType": "SALE",
    "referenceNumber": "INV-2026-00101",
    "quantityIn": 0,
    "quantityOut": 5,
    "runningBalance": 95
  }
]
```

---

### 3.6 `GET /api/reports/fast-moving-products`

**Purpose:** Top N products by quantity sold — identifies best sellers to ensure adequate stock.

**Query parameters:**

| Param | Type | Default | Description |
|-------|------|---------|-------------|
| `from` | `date` | today − 30 days | — |
| `to`   | `date` | today | — |
| `limit` | `int` | 10 | Max results (capped at 100) |

**Data sources:** `sale_lines`, `products`, `sales`

**Example response:**
```json
[
  {
    "productId": 8,
    "productName": "Tuborg Beer",
    "brand": "Tuborg",
    "category": "Beer",
    "quantitySold": 1200,
    "totalRevenue": 960000.00,
    "totalProfit": 144000.00,
    "lastSoldDate": "2026-03-13"
  }
]
```

Ordered by `quantitySold DESC`. Uses `.setMaxResults()` instead of SQL `LIMIT` for JPA compatibility.

---

### 3.7 `GET /api/reports/dead-stock`

**Purpose:** Products with stock > 0 that have had no sales in the last N days. Identifies inventory tying up capital.

**Query parameters:**

| Param | Type | Default | Description |
|-------|------|---------|-------------|
| `days` | `int` | 30 | Inactivity threshold |

**Data sources:** `products`, `sale_lines`, `sales`

Products that were **never sold** are also included (`lastSoldDate = null`).

**Example response:**
```json
[
  {
    "productId": 22,
    "productName": "Chivas Regal",
    "brand": "Chivas Brothers",
    "category": "Whiskey",
    "currentStock": 12,
    "stockValue": 114000.00,
    "lastSoldDate": "2026-02-01",
    "daysSinceLastSale": 40
  },
  {
    "productId": 31,
    "productName": "Imported Merlot",
    "brand": "XYZ",
    "category": "Wine",
    "currentStock": 6,
    "stockValue": 42000.00,
    "lastSoldDate": null,
    "daysSinceLastSale": null
  }
]
```

`stockValue = currentStock × product.average_cost`

---

### 3.8 `GET /api/reports/supplier-outstanding`

**Purpose:** Suppliers with unpaid purchase balances. Prioritises who to pay first.

**No query parameters.** Shows all active suppliers with outstanding > 0, ordered by outstanding balance descending.

**Data sources:** `suppliers`, `purchases`, `purchase_payments`

**Calculation:**
```
outstanding = SUM(purchases.invoice_amount) - SUM(purchase_payments.amount)
```
Both aggregated per `supplier_id` via subqueries — avoids cross-join multiplication.

**Example response:**
```json
[
  {
    "supplierId": 1,
    "supplierName": "Himalayan Distributor",
    "phone": "9801234567",
    "purchaseCount": 8,
    "totalPurchased": 672000.00,
    "totalPaid": 500000.00,
    "outstanding": 172000.00
  }
]
```

---

### 3.9 `GET /api/reports/dashboard`

**Purpose:** Owner dashboard — all key KPIs in a single request to minimise round trips.

**No query parameters.**

**Data sources:** All main tables — 7 separate queries aggregated in the service.

**Example response:**
```json
{
  "todaySales": 85000.00,
  "todayProfit": 15000.00,
  "todayInvoiceCount": 62,
  "lowStockCount": 6,
  "expiringCount": 3,
  "totalStockValue": 2450000.00,
  "pendingCustomerCredit": 200000.00,
  "pendingSupplierPayments": 172000.00
}
```

| Field | Calculation |
|-------|-------------|
| `todaySales` | `SUM(sales.total_amount)` WHERE date = today |
| `todayProfit` | `SUM(sl.quantity × (unit_price − cost_price_at_sale))` today |
| `lowStockCount` | products WHERE `current_stock < min_stock` AND `deleted = false` |
| `expiringCount` | DISTINCT product_ids in `batches` with `expiry_date ≤ today + 30 days` and `current_quantity > 0` |
| `totalStockValue` | `SUM(current_stock × average_cost)` all non-deleted products |
| `pendingCustomerCredit` | `SUM(customers.outstanding_balance)` |
| `pendingSupplierPayments` | `SUM(purchases.invoice_amount) − SUM(purchase_payments.amount)` |

---

### 3.10 `GET /api/reports/category-sales`

**Purpose:** Sales performance by product category — shows revenue mix (Beer vs Whiskey vs Rum etc.).

**Query parameters:** `from`, `to`

**Data sources:** `sale_lines`, `products`, `sales`

**Example response:**
```json
[
  { "category": "Whiskey",  "quantitySold": 820,  "revenue": 1180000.00, "profit": 210000.00, "revenuePct": 40.1 },
  { "category": "Beer",     "quantitySold": 3200, "revenue": 1030000.00, "profit": 154500.00, "revenuePct": 35.0 },
  { "category": "Rum",      "quantitySold": 450,  "revenue": 441000.00,  "profit": 79380.00,  "revenuePct": 15.0 },
  { "category": "Vodka",    "quantitySold": 310,  "revenue": 290000.00,  "profit": 52200.00,  "revenuePct": 9.9  }
]
```

`revenuePct` is computed in Java to avoid rounding drift. Products with null category appear as `"Uncategorized"`.

---

## 4. Query Logic Notes

### Avoiding double-counting

When `sales` is JOINed with `sale_lines`, each sale row is repeated once per line item. Naive `SUM(s.total_amount)` would multiply the amount. Two strategies are used:

1. **Subquery separation** — `daily-sales` uses one subquery for sale-level sums and a second for line-level profit, then LEFT JOINs by date.
2. **Single-table aggregation** — other reports aggregate directly from `sale_lines` (never from `sales.total_amount`), so no duplicate rows occur.

### Profit calculation

All profit uses `cost_price_at_sale` (WAC snapshot at time of sale):

```
profit = SUM(quantity × (unit_price − cost_price_at_sale))
```

This is the correct approach because `product.average_cost` changes on every new purchase. The snapshot on `sale_lines` is the only reliable historical cost.

### Date parameters

- `sale_date` is `TIMESTAMP` → passed as `LocalDateTime` (midnight boundaries): `from.atStartOfDay()` to `to.plusDays(1).atStartOfDay()`
- `purchase_date` and `batches.purchase_date` are `DATE` → passed as `LocalDate` directly
- All comparisons use `>=` lower bound and `<` upper bound for TIMESTAMP ranges to avoid fence-post errors

### Dead stock HAVING clause

```sql
HAVING MAX(s.sale_date) IS NULL
    OR MAX(s.sale_date) < :cutoffTs
```

- `IS NULL` catches products that were never sold (LEFT JOIN returns no sale rows → MAX is NULL)
- The cutoff comparison uses TIMESTAMP for correct time-zone-aware boundaries

---

## 5. Existing APIs Not Duplicated

The following are already covered by Phase 1 APIs:

| Existing API | What it provides |
|-------------|-----------------|
| `GET /api/inventory` | Current stock qty, avg cost, stock value per product |
| `GET /api/inventory/low-stock` | Low stock alert list |
| `GET /api/inventory/expiring?days=N` | Expiring batch list |
| `GET /api/customers/{id}/statement` | Per-customer sales + payment journal |
| `GET /api/purchases/{id}/payments` | Per-purchase payment history |

The reporting module's `/dashboard` and `/dead-stock` complement (not duplicate) these.

---

## 6. Performance Notes

- All report queries run in `readOnly = true` transactions
- Indexes already on `sales.sale_date`, `sale_lines.product_id`, `purchase_lines.product_id`, `batches.product_id`, `batches.expiry_date`
- For shops with > 50,000 sales records, add a composite index: `CREATE INDEX ON sales(CAST(sale_date AS DATE))` to speed up the daily-sales subquery
- The `fast-moving-products` endpoint is capped at `limit = 100` rows maximum
- `supplier-outstanding` and `dead-stock` have no pagination — acceptable given the expected entity count for a single-store system
