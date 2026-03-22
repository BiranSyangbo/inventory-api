package com.liquorshop.inventory.dto;

import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
public class DashboardResponse {
    private BigDecimal todaySales;
    private BigDecimal todayProfit;
    private int todayInvoiceCount;
    private int lowStockCount;
    /** Products with at least one batch expiring within 30 days */
    private int expiringCount;
    private BigDecimal totalStockValue;
    /** Sum of all customer outstanding_balance */
    private BigDecimal pendingCustomerCredit;
    /** Total purchase invoices minus total purchase payments made */
    private BigDecimal pendingSupplierPayments;
}
