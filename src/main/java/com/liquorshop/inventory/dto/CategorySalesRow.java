package com.liquorshop.inventory.dto;

import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
public class CategorySalesRow {
    private String category;
    private int quantitySold;
    private BigDecimal revenue;
    private BigDecimal profit;
    /** Revenue share as a percentage of total (0–100) */
    private BigDecimal revenuePct;
}
