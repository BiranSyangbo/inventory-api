package com.liquorshop.inventory.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class ProductRequest {

    @NotBlank(message = "Product name is required")
    private String name;

    private String brand;
    private String category;
    private Integer volumeMl;
    private String unit;
    private String barcode;

    @Min(value = 0, message = "Minimum stock cannot be negative")
    private Integer minStock = 0;

    @NotNull(message = "Selling price is required")
    @Min(value = 0, message = "Selling price cannot be negative")
    private BigDecimal sellingPrice;

    private String status = "ACTIVE";
}
