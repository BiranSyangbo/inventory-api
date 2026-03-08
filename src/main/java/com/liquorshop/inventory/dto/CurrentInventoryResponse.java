package com.liquorshop.inventory.dto;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class CurrentInventoryResponse {

    private Long productId;
    private String name;
    private String brand;
    private String category;
    private Integer volumeMl;
    private String unit;
    private Integer minStock;
    private Integer totalQuantity;
    private BigDecimal averageCost;
    private BigDecimal sellingPrice;
    private BigDecimal totalValue; // totalQuantity × averageCost
    private Boolean isLowStock;
}
