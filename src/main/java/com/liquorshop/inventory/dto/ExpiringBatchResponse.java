package com.liquorshop.inventory.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
public class ExpiringBatchResponse {

    private Long id;
    private Long productId;
    private String productName;
    private String productBrand;
    private String batchCode;
    private LocalDate expiryDate;
    private BigDecimal purchasePrice;
    private Integer currentQuantity;
    private String location;
    private LocalDateTime createdAt;
    private String status; // "expired" | "expiring_soon"
}
