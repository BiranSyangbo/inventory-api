package com.liquorshop.inventory.dto;

import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@NoArgsConstructor
public class DailySalesRow {
    private LocalDate date;
    private long invoiceCount;
    private BigDecimal totalSales;
    private BigDecimal totalProfit;
    private BigDecimal totalVat;
    private BigDecimal walkInSales;
    private BigDecimal customerSales;
}
