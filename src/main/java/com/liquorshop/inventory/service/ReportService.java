package com.liquorshop.inventory.service;

import com.liquorshop.inventory.dto.*;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
@Transactional(readOnly = true)
public class ReportService {

    @PersistenceContext
    private EntityManager em;

    // ─── Daily Sales ───────────────────────────────────────────────────────────

    /**
     * Returns one row per calendar day in [from, to] that had sales.
     * Avoids double-counting by separating sale-level aggregation from
     * sale_lines profit aggregation via subqueries.
     */
    @SuppressWarnings("unchecked")
    public List<DailySalesRow> getDailySales(LocalDate from, LocalDate to) {
        // sale_date is TIMESTAMP; compare against midnight boundaries
        String sql = """
                SELECT
                  d.sale_date,
                  d.invoice_count,
                  d.total_sales,
                  d.total_vat,
                  d.walk_in_sales,
                  d.customer_sales,
                  COALESCE(p.total_profit, 0) AS total_profit
                FROM (
                  SELECT
                    CAST(s.sale_date AS DATE)                        AS sale_date,
                    COUNT(s.id)                                      AS invoice_count,
                    SUM(s.total_amount)                              AS total_sales,
                    COALESCE(SUM(s.vat_amount), 0)                   AS total_vat,
                    SUM(CASE WHEN s.customer_id IS NULL
                             THEN s.total_amount ELSE 0 END)         AS walk_in_sales,
                    SUM(CASE WHEN s.customer_id IS NOT NULL
                             THEN s.total_amount ELSE 0 END)         AS customer_sales
                  FROM sales s
                  WHERE s.sale_date >= :fromTs AND s.sale_date < :toTs
                  GROUP BY CAST(s.sale_date AS DATE)
                ) d
                LEFT JOIN (
                  SELECT
                    CAST(s.sale_date AS DATE) AS sale_date,
                    SUM(sl.quantity * (sl.unit_price - sl.cost_price_at_sale)) AS total_profit
                  FROM sales s
                  JOIN sale_lines sl ON s.id = sl.sale_id
                  WHERE s.sale_date >= :fromTs AND s.sale_date < :toTs
                  GROUP BY CAST(s.sale_date AS DATE)
                ) p ON p.sale_date = d.sale_date
                ORDER BY d.sale_date
                """;

        List<Object[]> rows = em.createNativeQuery(sql)
                .setParameter("fromTs", from.atStartOfDay())
                .setParameter("toTs", to.plusDays(1).atStartOfDay())
                .getResultList();

        return rows.stream().map(r -> {
            DailySalesRow row = new DailySalesRow();
            row.setDate(toLocalDate(r[0]));
            row.setInvoiceCount(toLong(r[1]));
            row.setTotalSales(toBd(r[2]));
            row.setTotalVat(toBd(r[3]));
            row.setWalkInSales(toBd(r[4]));
            row.setCustomerSales(toBd(r[5]));
            row.setTotalProfit(toBd(r[6]));
            return row;
        }).collect(Collectors.toList());
    }

    // ─── Profit & Loss ─────────────────────────────────────────────────────────

    /**
     * Per-product profit analysis using cost_price_at_sale (WAC snapshot).
     * Profit = (unit_price - cost_price_at_sale) * quantity
     */
    @SuppressWarnings("unchecked")
    public List<ProfitLossRow> getProfitLoss(LocalDate from, LocalDate to) {
        String sql = """
                SELECT
                  p.id                                                        AS product_id,
                  p.name                                                      AS product_name,
                  p.brand,
                  p.category,
                  SUM(sl.quantity)                                            AS quantity_sold,
                  SUM(sl.line_total)                                          AS revenue,
                  SUM(sl.quantity * sl.cost_price_at_sale)                   AS total_cost,
                  SUM(sl.quantity * (sl.unit_price - sl.cost_price_at_sale)) AS profit
                FROM sale_lines sl
                JOIN products p ON sl.product_id = p.id
                JOIN sales   s ON sl.sale_id    = s.id
                WHERE s.sale_date >= :fromTs AND s.sale_date < :toTs
                GROUP BY p.id, p.name, p.brand, p.category
                ORDER BY profit DESC
                """;

        List<Object[]> rows = em.createNativeQuery(sql)
                .setParameter("fromTs", from.atStartOfDay())
                .setParameter("toTs", to.plusDays(1).atStartOfDay())
                .getResultList();

        return rows.stream().map(r -> {
            ProfitLossRow row = new ProfitLossRow();
            row.setProductId(toLong(r[0]));
            row.setProductName((String) r[1]);
            row.setBrand((String) r[2]);
            row.setCategory((String) r[3]);
            row.setQuantitySold(toInt(r[4]));
            row.setRevenue(toBd(r[5]));
            row.setTotalCost(toBd(r[6]));
            row.setProfit(toBd(r[7]));
            BigDecimal rev = row.getRevenue();
            row.setMarginPct(rev.compareTo(BigDecimal.ZERO) > 0
                    ? row.getProfit().multiply(BigDecimal.valueOf(100)).divide(rev, 2, RoundingMode.HALF_UP)
                    : BigDecimal.ZERO);
            return row;
        }).collect(Collectors.toList());
    }

    // ─── Purchase Report ───────────────────────────────────────────────────────

    @SuppressWarnings("unchecked")
    public List<PurchaseReportRow> getPurchaseReport(LocalDate from, LocalDate to, Long supplierId) {
        StringBuilder sql = new StringBuilder("""
                SELECT
                  pu.id,
                  pu.purchase_date,
                  s.name                        AS supplier_name,
                  pu.vat_bill_number,
                  COALESCE(pu.invoice_amount, 0) AS invoice_amount,
                  COALESCE(pu.vat_amount,     0) AS vat_amount,
                  COALESCE(pu.discount,       0) AS discount,
                  COALESCE(pp_agg.total_paid, 0) AS total_paid
                FROM purchases pu
                JOIN suppliers s ON pu.supplier_id = s.id
                LEFT JOIN (
                  SELECT purchase_id, SUM(amount) AS total_paid
                  FROM purchase_payments
                  GROUP BY purchase_id
                ) pp_agg ON pp_agg.purchase_id = pu.id
                WHERE pu.purchase_date >= :fromDate AND pu.purchase_date <= :toDate
                """);

        if (supplierId != null) {
            sql.append(" AND pu.supplier_id = :supplierId");
        }
        sql.append(" ORDER BY pu.purchase_date DESC");

        var query = em.createNativeQuery(sql.toString())
                .setParameter("fromDate", from)
                .setParameter("toDate", to);
        if (supplierId != null) {
            query.setParameter("supplierId", supplierId);
        }

        List<Object[]> rows = query.getResultList();
        return rows.stream().map(r -> {
            PurchaseReportRow row = new PurchaseReportRow();
            row.setPurchaseId(toLong(r[0]));
            row.setPurchaseDate(toLocalDate(r[1]));
            row.setSupplierName((String) r[2]);
            row.setVatBillNumber((String) r[3]);
            row.setInvoiceAmount(toBd(r[4]));
            row.setVatAmount(toBd(r[5]));
            row.setDiscount(toBd(r[6]));
            row.setTotalPaid(toBd(r[7]));
            row.setOutstanding(row.getInvoiceAmount().subtract(row.getTotalPaid()));
            return row;
        }).collect(Collectors.toList());
    }

    // ─── VAT Report ────────────────────────────────────────────────────────────

    @SuppressWarnings("unchecked")
    public VatReportResponse getVatReport(LocalDate from, LocalDate to) {
        // Sales with VAT
        List<Object[]> saleRows = em.createNativeQuery("""
                SELECT s.id, CAST(s.sale_date AS DATE), s.invoice_number,
                       s.total_amount, COALESCE(s.vat_amount, 0)
                FROM sales s
                WHERE s.sale_date >= :fromTs AND s.sale_date < :toTs
                  AND COALESCE(s.vat_amount, 0) > 0
                ORDER BY s.sale_date
                """)
                .setParameter("fromTs", from.atStartOfDay())
                .setParameter("toTs", to.plusDays(1).atStartOfDay())
                .getResultList();

        List<VatSalesLine> salesLines = saleRows.stream().map(r -> {
            VatSalesLine line = new VatSalesLine();
            line.setSaleId(toLong(r[0]));
            line.setSaleDate(toLocalDate(r[1]));
            line.setInvoiceNumber((String) r[2]);
            line.setTotalAmount(toBd(r[3]));
            line.setVatAmount(toBd(r[4]));
            return line;
        }).collect(Collectors.toList());

        // Purchases with VAT
        List<Object[]> purchaseRows = em.createNativeQuery("""
                SELECT pu.id, pu.purchase_date, s.name, pu.vat_bill_number,
                       COALESCE(pu.invoice_amount, 0), COALESCE(pu.vat_amount, 0)
                FROM purchases pu
                JOIN suppliers s ON pu.supplier_id = s.id
                WHERE pu.purchase_date >= :fromDate AND pu.purchase_date <= :toDate
                  AND COALESCE(pu.vat_amount, 0) > 0
                ORDER BY pu.purchase_date
                """)
                .setParameter("fromDate", from)
                .setParameter("toDate", to)
                .getResultList();

        List<VatPurchaseLine> purchaseLines = purchaseRows.stream().map(r -> {
            VatPurchaseLine line = new VatPurchaseLine();
            line.setPurchaseId(toLong(r[0]));
            line.setPurchaseDate(toLocalDate(r[1]));
            line.setSupplierName((String) r[2]);
            line.setVatBillNumber((String) r[3]);
            line.setInvoiceAmount(toBd(r[4]));
            line.setVatAmount(toBd(r[5]));
            return line;
        }).collect(Collectors.toList());

        BigDecimal totalSalesVat = salesLines.stream()
                .map(VatSalesLine::getVatAmount).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal totalPurchaseVat = purchaseLines.stream()
                .map(VatPurchaseLine::getVatAmount).reduce(BigDecimal.ZERO, BigDecimal::add);

        VatReportResponse response = new VatReportResponse();
        response.setTotalSalesVat(totalSalesVat);
        response.setTotalPurchaseVat(totalPurchaseVat);
        response.setNetVatLiability(totalSalesVat.subtract(totalPurchaseVat));
        response.setSalesLines(salesLines);
        response.setPurchaseLines(purchaseLines);
        return response;
    }

    // ─── Stock Movement ────────────────────────────────────────────────────────

    /**
     * Full stock movement history for a product: purchases (IN) and sales (OUT).
     * Running balance is computed in application memory.
     */
    @SuppressWarnings("unchecked")
    public List<StockMovementRow> getStockMovement(Long productId, LocalDate from, LocalDate to) {
        // purchase_date on batches is DATE; sale_date on sales is TIMESTAMP
        String sql = """
                SELECT move_date, transaction_type, reference_number, qty_in, qty_out
                FROM (
                  SELECT
                    b.purchase_date                AS move_date,
                    'PURCHASE'                     AS transaction_type,
                    COALESCE(pu.vat_bill_number, CAST(pu.id AS VARCHAR)) AS reference_number,
                    pl.quantity                    AS qty_in,
                    0                              AS qty_out
                  FROM purchase_lines pl
                  JOIN purchases pu ON pl.purchase_id = pu.id
                  JOIN batches   b  ON pl.batch_id    = b.id
                  WHERE pl.product_id = :productId
                    AND b.purchase_date >= :fromDate AND b.purchase_date <= :toDate

                  UNION ALL

                  SELECT
                    CAST(s.sale_date AS DATE)      AS move_date,
                    'SALE'                         AS transaction_type,
                    s.invoice_number               AS reference_number,
                    0                              AS qty_in,
                    sl.quantity                    AS qty_out
                  FROM sale_lines sl
                  JOIN sales s ON sl.sale_id = s.id
                  WHERE sl.product_id = :productId
                    AND s.sale_date >= :fromTs AND s.sale_date < :toTs
                ) movements
                ORDER BY move_date, transaction_type
                """;

        List<Object[]> rows = em.createNativeQuery(sql)
                .setParameter("productId", productId)
                .setParameter("fromDate", from)
                .setParameter("toDate", to)
                .setParameter("fromTs", from.atStartOfDay())
                .setParameter("toTs", to.plusDays(1).atStartOfDay())
                .getResultList();

        List<StockMovementRow> result = new ArrayList<>();
        int balance = 0;
        for (Object[] r : rows) {
            StockMovementRow row = new StockMovementRow();
            row.setDate(toLocalDate(r[0]));
            row.setTransactionType((String) r[1]);
            row.setReferenceNumber((String) r[2]);
            row.setQuantityIn(toInt(r[3]));
            row.setQuantityOut(toInt(r[4]));
            balance += row.getQuantityIn() - row.getQuantityOut();
            row.setRunningBalance(balance);
            result.add(row);
        }
        return result;
    }

    // ─── Fast Moving Products ──────────────────────────────────────────────────

    @SuppressWarnings("unchecked")
    public List<FastMovingProductRow> getFastMovingProducts(LocalDate from, LocalDate to, int limit) {
        String sql = """
                SELECT
                  p.id,
                  p.name,
                  p.brand,
                  p.category,
                  SUM(sl.quantity)                                            AS quantity_sold,
                  SUM(sl.line_total)                                          AS total_revenue,
                  SUM(sl.quantity * (sl.unit_price - sl.cost_price_at_sale)) AS total_profit,
                  MAX(CAST(s.sale_date AS DATE))                             AS last_sold_date
                FROM sale_lines sl
                JOIN products p ON sl.product_id = p.id
                JOIN sales   s ON sl.sale_id    = s.id
                WHERE s.sale_date >= :fromTs AND s.sale_date < :toTs
                GROUP BY p.id, p.name, p.brand, p.category
                ORDER BY quantity_sold DESC
                """;

        List<Object[]> rows = em.createNativeQuery(sql)
                .setParameter("fromTs", from.atStartOfDay())
                .setParameter("toTs", to.plusDays(1).atStartOfDay())
                .setMaxResults(limit)
                .getResultList();

        return rows.stream().map(r -> {
            FastMovingProductRow row = new FastMovingProductRow();
            row.setProductId(toLong(r[0]));
            row.setProductName((String) r[1]);
            row.setBrand((String) r[2]);
            row.setCategory((String) r[3]);
            row.setQuantitySold(toInt(r[4]));
            row.setTotalRevenue(toBd(r[5]));
            row.setTotalProfit(toBd(r[6]));
            row.setLastSoldDate(toLocalDate(r[7]));
            return row;
        }).collect(Collectors.toList());
    }

    // ─── Dead Stock ────────────────────────────────────────────────────────────

    /**
     * Products currently in stock that have had no sales in the last {@code days} days.
     * Includes products that were never sold.
     */
    @SuppressWarnings("unchecked")
    public List<DeadStockRow> getDeadStock(int days) {
        LocalDate cutoff = LocalDate.now().minusDays(days);
        String sql = """
                SELECT
                  p.id,
                  p.name,
                  p.brand,
                  p.category,
                  p.current_stock,
                  (CAST(p.current_stock AS DECIMAL) * p.average_cost) AS stock_value,
                  MAX(CAST(s.sale_date AS DATE))                       AS last_sold_date
                FROM products p
                LEFT JOIN sale_lines sl ON p.id = sl.product_id
                LEFT JOIN sales      s  ON sl.sale_id = s.id
                WHERE p.deleted = false AND p.current_stock > 0
                GROUP BY p.id, p.name, p.brand, p.category, p.current_stock, p.average_cost
                HAVING MAX(s.sale_date) IS NULL
                    OR MAX(s.sale_date) < :cutoffTs
                ORDER BY last_sold_date NULLS FIRST, p.name
                """;

        List<Object[]> rows = em.createNativeQuery(sql)
                .setParameter("cutoffTs", cutoff.atStartOfDay())
                .getResultList();

        LocalDate today = LocalDate.now();
        return rows.stream().map(r -> {
            DeadStockRow row = new DeadStockRow();
            row.setProductId(toLong(r[0]));
            row.setProductName((String) r[1]);
            row.setBrand((String) r[2]);
            row.setCategory((String) r[3]);
            row.setCurrentStock(toInt(r[4]));
            row.setStockValue(toBd(r[5]));
            LocalDate lastSold = toLocalDate(r[6]);
            row.setLastSoldDate(lastSold);
            row.setDaysSinceLastSale(lastSold != null
                    ? (int) ChronoUnit.DAYS.between(lastSold, today)
                    : null);
            return row;
        }).collect(Collectors.toList());
    }

    // ─── Supplier Outstanding ──────────────────────────────────────────────────

    @SuppressWarnings("unchecked")
    public List<SupplierOutstandingRow> getSupplierOutstanding() {
        String sql = """
                SELECT
                  sup.id,
                  sup.name,
                  sup.phone,
                  COALESCE(pu_agg.purchase_count,  0) AS purchase_count,
                  COALESCE(pu_agg.total_purchased, 0) AS total_purchased,
                  COALESCE(pp_agg.total_paid,      0) AS total_paid
                FROM suppliers sup
                LEFT JOIN (
                  SELECT supplier_id,
                         COUNT(*)          AS purchase_count,
                         SUM(invoice_amount) AS total_purchased
                  FROM purchases
                  GROUP BY supplier_id
                ) pu_agg ON pu_agg.supplier_id = sup.id
                LEFT JOIN (
                  SELECT supplier_id, SUM(amount) AS total_paid
                  FROM purchase_payments
                  GROUP BY supplier_id
                ) pp_agg ON pp_agg.supplier_id = sup.id
                WHERE sup.status = 'ACTIVE'
                  AND COALESCE(pu_agg.total_purchased, 0)
                    - COALESCE(pp_agg.total_paid,      0) > 0
                ORDER BY (COALESCE(pu_agg.total_purchased, 0)
                         - COALESCE(pp_agg.total_paid,     0)) DESC
                """;

        List<Object[]> rows = em.createNativeQuery(sql).getResultList();
        return rows.stream().map(r -> {
            SupplierOutstandingRow row = new SupplierOutstandingRow();
            row.setSupplierId(toLong(r[0]));
            row.setSupplierName((String) r[1]);
            row.setPhone((String) r[2]);
            row.setPurchaseCount(toInt(r[3]));
            row.setTotalPurchased(toBd(r[4]));
            row.setTotalPaid(toBd(r[5]));
            row.setOutstanding(row.getTotalPurchased().subtract(row.getTotalPaid()));
            return row;
        }).collect(Collectors.toList());
    }

    // ─── Category Sales ────────────────────────────────────────────────────────

    @SuppressWarnings("unchecked")
    public List<CategorySalesRow> getCategorySales(LocalDate from, LocalDate to) {
        String sql = """
                SELECT
                  COALESCE(p.category, 'Uncategorized')                      AS category,
                  SUM(sl.quantity)                                            AS quantity_sold,
                  SUM(sl.line_total)                                          AS revenue,
                  SUM(sl.quantity * (sl.unit_price - sl.cost_price_at_sale)) AS profit
                FROM sale_lines sl
                JOIN products p ON sl.product_id = p.id
                JOIN sales   s ON sl.sale_id    = s.id
                WHERE s.sale_date >= :fromTs AND s.sale_date < :toTs
                GROUP BY COALESCE(p.category, 'Uncategorized')
                ORDER BY revenue DESC
                """;

        List<Object[]> rows = em.createNativeQuery(sql)
                .setParameter("fromTs", from.atStartOfDay())
                .setParameter("toTs", to.plusDays(1).atStartOfDay())
                .getResultList();

        List<CategorySalesRow> result = rows.stream().map(r -> {
            CategorySalesRow row = new CategorySalesRow();
            row.setCategory((String) r[0]);
            row.setQuantitySold(toInt(r[1]));
            row.setRevenue(toBd(r[2]));
            row.setProfit(toBd(r[3]));
            return row;
        }).collect(Collectors.toList());

        BigDecimal totalRevenue = result.stream()
                .map(CategorySalesRow::getRevenue).reduce(BigDecimal.ZERO, BigDecimal::add);

        if (totalRevenue.compareTo(BigDecimal.ZERO) > 0) {
            result.forEach(r -> r.setRevenuePct(
                    r.getRevenue().multiply(BigDecimal.valueOf(100))
                            .divide(totalRevenue, 1, RoundingMode.HALF_UP)));
        }
        return result;
    }

    // ─── Dashboard ─────────────────────────────────────────────────────────────

    public DashboardResponse getDashboard() {
        DashboardResponse resp = new DashboardResponse();
        LocalDate today = LocalDate.now();

        // Today's sales (sale-level aggregation — no join needed)
        Object[] salesRow = (Object[]) em.createNativeQuery("""
                SELECT COALESCE(SUM(total_amount), 0), COUNT(id)
                FROM sales
                WHERE CAST(sale_date AS DATE) = :today
                """)
                .setParameter("today", today)
                .getSingleResult();
        resp.setTodaySales(toBd(salesRow[0]));
        resp.setTodayInvoiceCount(toInt(salesRow[1]));

        // Today's profit (from sale_lines, separate to avoid double-counting)
        Object profitResult = em.createNativeQuery("""
                SELECT COALESCE(SUM(sl.quantity * (sl.unit_price - sl.cost_price_at_sale)), 0)
                FROM sale_lines sl
                JOIN sales s ON sl.sale_id = s.id
                WHERE CAST(s.sale_date AS DATE) = :today
                """)
                .setParameter("today", today)
                .getSingleResult();
        resp.setTodayProfit(toBd(profitResult));

        // Low stock count
        Object lowStock = em.createNativeQuery(
                "SELECT COUNT(*) FROM products WHERE current_stock < min_stock AND deleted = false")
                .getSingleResult();
        resp.setLowStockCount(((Number) lowStock).intValue());

        // Products with batch expiring within 30 days (at least one unit remaining)
        Object expiring = em.createNativeQuery("""
                SELECT COUNT(DISTINCT product_id)
                FROM batches
                WHERE expiry_date IS NOT NULL
                  AND expiry_date <= CURRENT_DATE + INTERVAL '30 days'
                  AND current_quantity > 0
                """)
                .getSingleResult();
        resp.setExpiringCount(((Number) expiring).intValue());

        // Total stock value based on current WAC
        Object stockVal = em.createNativeQuery("""
                SELECT COALESCE(SUM(CAST(current_stock AS DECIMAL) * average_cost), 0)
                FROM products
                WHERE deleted = false
                """)
                .getSingleResult();
        resp.setTotalStockValue(toBd(stockVal));

        // Sum of all customer outstanding balances
        Object creditPending = em.createNativeQuery(
                "SELECT COALESCE(SUM(outstanding_balance), 0) FROM customers")
                .getSingleResult();
        resp.setPendingCustomerCredit(toBd(creditPending));

        // Total purchase amount minus total purchase payments
        Object supplierPending = em.createNativeQuery("""
                SELECT
                  (SELECT COALESCE(SUM(invoice_amount), 0) FROM purchases)
                - (SELECT COALESCE(SUM(amount),         0) FROM purchase_payments)
                """)
                .getSingleResult();
        resp.setPendingSupplierPayments(toBd(supplierPending));

        return resp;
    }

    // ─── Type-safe helpers ─────────────────────────────────────────────────────

    private BigDecimal toBd(Object obj) {
        if (obj == null) return BigDecimal.ZERO;
        if (obj instanceof BigDecimal bd) return bd;
        if (obj instanceof Number n) return BigDecimal.valueOf(n.doubleValue());
        return BigDecimal.ZERO;
    }

    private Long toLong(Object obj) {
        if (obj == null) return null;
        if (obj instanceof Long l) return l;
        if (obj instanceof Number n) return n.longValue();
        return null;
    }

    private Integer toInt(Object obj) {
        if (obj == null) return 0;
        if (obj instanceof Integer i) return i;
        if (obj instanceof Number n) return n.intValue();
        return 0;
    }

    private LocalDate toLocalDate(Object obj) {
        if (obj == null) return null;
        if (obj instanceof LocalDate ld) return ld;
        if (obj instanceof java.sql.Date d) return d.toLocalDate();
        if (obj instanceof java.util.Date d) return d.toInstant()
                .atZone(java.time.ZoneId.systemDefault()).toLocalDate();
        return null;
    }
}
