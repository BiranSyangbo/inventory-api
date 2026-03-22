package com.liquorshop.inventory.controller;

import com.liquorshop.inventory.dto.*;
import com.liquorshop.inventory.service.ReportService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/reports")
@RequiredArgsConstructor
public class ReportController {

    private final ReportService reportService;

    /**
     * Daily sales summary — one row per calendar day in the range.
     * Includes total sales, profit, VAT, walk-in vs customer split.
     */
    @GetMapping("/daily-sales")
    public ResponseEntity<List<DailySalesRow>> getDailySales(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        if (from == null) from = LocalDate.now().minusDays(30);
        if (to == null) to = LocalDate.now();
        return ResponseEntity.ok(reportService.getDailySales(from, to));
    }

    /**
     * Per-product profit & loss — revenue, cost (WAC), profit, margin %.
     */
    @GetMapping("/profit-loss")
    public ResponseEntity<List<ProfitLossRow>> getProfitLoss(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        if (from == null) from = LocalDate.now().minusDays(30);
        if (to == null) to = LocalDate.now();
        return ResponseEntity.ok(reportService.getProfitLoss(from, to));
    }

    /**
     * Purchase history with per-purchase payment totals and outstanding balance.
     * Optional supplierId filter.
     */
    @GetMapping("/purchase-report")
    public ResponseEntity<List<PurchaseReportRow>> getPurchaseReport(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(required = false) Long supplierId) {
        if (from == null) from = LocalDate.now().minusDays(30);
        if (to == null) to = LocalDate.now();
        return ResponseEntity.ok(reportService.getPurchaseReport(from, to, supplierId));
    }

    /**
     * VAT report — purchase VAT input, sales VAT output, net liability.
     */
    @GetMapping("/vat-report")
    public ResponseEntity<VatReportResponse> getVatReport(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        if (from == null) from = LocalDate.now().minusDays(30);
        if (to == null) to = LocalDate.now();
        return ResponseEntity.ok(reportService.getVatReport(from, to));
    }

    /**
     * Full stock movement for a single product — purchases IN, sales OUT,
     * with a running balance column.
     */
    @GetMapping("/stock-movement")
    public ResponseEntity<List<StockMovementRow>> getStockMovement(
            @RequestParam Long productId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        if (from == null) from = LocalDate.now().minusDays(90);
        if (to == null) to = LocalDate.now();
        return ResponseEntity.ok(reportService.getStockMovement(productId, from, to));
    }

    /**
     * Top N products by quantity sold in the period. Default limit = 10.
     */
    @GetMapping("/fast-moving-products")
    public ResponseEntity<List<FastMovingProductRow>> getFastMovingProducts(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(defaultValue = "10") int limit) {
        if (from == null) from = LocalDate.now().minusDays(30);
        if (to == null) to = LocalDate.now();
        return ResponseEntity.ok(reportService.getFastMovingProducts(from, to, Math.min(limit, 100)));
    }

    /**
     * Products with current stock > 0 that have had no sales in the last
     * {@code days} days (default 30). Products never sold are included.
     */
    @GetMapping("/dead-stock")
    public ResponseEntity<List<DeadStockRow>> getDeadStock(
            @RequestParam(defaultValue = "30") int days) {
        return ResponseEntity.ok(reportService.getDeadStock(days));
    }

    /**
     * Suppliers with unpaid invoice balances, ordered by outstanding amount.
     */
    @GetMapping("/supplier-outstanding")
    public ResponseEntity<List<SupplierOutstandingRow>> getSupplierOutstanding() {
        return ResponseEntity.ok(reportService.getSupplierOutstanding());
    }

    /**
     * Owner dashboard — today's KPIs plus inventory and credit health summary.
     */
    @GetMapping("/dashboard")
    public ResponseEntity<DashboardResponse> getDashboard() {
        return ResponseEntity.ok(reportService.getDashboard());
    }

    /**
     * Sales grouped by product category with revenue share %.
     */
    @GetMapping("/category-sales")
    public ResponseEntity<List<CategorySalesRow>> getCategorySales(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        if (from == null) from = LocalDate.now().minusDays(30);
        if (to == null) to = LocalDate.now();
        return ResponseEntity.ok(reportService.getCategorySales(from, to));
    }
}
