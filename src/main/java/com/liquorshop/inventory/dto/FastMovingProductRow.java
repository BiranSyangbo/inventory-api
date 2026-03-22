package com.liquorshop.inventory.dto;

import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@NoArgsConstructor
public class FastMovingProductRow {
    private Long productId;
    private String productName;
    private String brand;
    private String category;
    private int quantitySold;
    private BigDecimal totalRevenue;
    private BigDecimal totalProfit;
    private LocalDate lastSoldDate;
}
