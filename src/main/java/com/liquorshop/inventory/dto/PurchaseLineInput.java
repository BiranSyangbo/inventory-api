package com.liquorshop.inventory.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
public class PurchaseLineInput {

    @NotNull(message = "Product ID is required")
    private Long productId;

    private String batchCode;

    private LocalDate expiryDate;

    @NotNull(message = "Purchase price is required")
    @Min(value = 0, message = "Purchase price cannot be negative")
    private BigDecimal purchasePrice;

    @NotNull(message = "Quantity is required")
    @Min(value = 1, message = "Quantity must be at least 1")
    private Integer quantity;

    private BigDecimal vatPercent = BigDecimal.ZERO;

    private String location;
}
