package com.liquorshop.inventory.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ExpiringBatchResponse {

    private Long id;
    private Long productId;
    private String batchCode;
    private String expiryDate;
    private BigDecimal purchasePrice;
    private BigDecimal sellingPrice;
    private Integer currentQuantity;
    private String location;
    private LocalDateTime createdAt;
    private String productName;
    private String productBrand;
    private String status; // "expired" or "expiring_soon"
}
