package com.liquorshop.inventory.dto;

import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@NoArgsConstructor
public class PurchaseReportRow {
    private Long purchaseId;
    private LocalDate purchaseDate;
    private String supplierName;
    private String vatBillNumber;
    private BigDecimal invoiceAmount;
    private BigDecimal vatAmount;
    private BigDecimal discount;
    private BigDecimal totalPaid;
    private BigDecimal outstanding;
}
