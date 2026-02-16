package com.liquorshop.inventory.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CurrentInventoryResponse {

    private Long productId;
    private String name;
    private String brand;
    private String category;
    private Integer volumeMl;
    private String unit;
    private Integer minStock;
    private Integer totalQuantity;
    private BigDecimal totalValue;
    private Boolean isLowStock;
}
