package com.liquorshop.inventory.dto;

import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
public class SupplierOutstandingRow {
    private Long supplierId;
    private String supplierName;
    private String phone;
    private int purchaseCount;
    private BigDecimal totalPurchased;
    private BigDecimal totalPaid;
    private BigDecimal outstanding;
}
