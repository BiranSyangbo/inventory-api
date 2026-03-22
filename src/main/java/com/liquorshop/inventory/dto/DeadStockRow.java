package com.liquorshop.inventory.dto;

import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@NoArgsConstructor
public class DeadStockRow {
    private Long productId;
    private String productName;
    private String brand;
    private String category;
    private int currentStock;
    private BigDecimal stockValue;
    /** Null means the product was never sold */
    private LocalDate lastSoldDate;
    /** Null means the product was never sold */
    private Integer daysSinceLastSale;
}
