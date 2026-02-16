package com.liquorshop.inventory.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Min;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PurchaseLineInput {

    @NotNull(message = "product_id is required")
    private Long productId;

    private String batchCode;

    private String expiryDate;

    @NotNull(message = "purchase_price is required")
    @Min(value = 0, message = "purchase_price cannot be negative")
    private BigDecimal purchasePrice;

    @NotNull(message = "selling_price is required")
    @Min(value = 0, message = "selling_price cannot be negative")
    private BigDecimal sellingPrice;

    @NotNull(message = "quantity is required")
    @Min(value = 1, message = "quantity must be at least 1")
    private Integer quantity;

    private String location;
}
