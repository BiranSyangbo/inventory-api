package com.liquorshop.inventory.dto;

import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
public class ProfitLossRow {
    private Long productId;
    private String productName;
    private String brand;
    private String category;
    private int quantitySold;
    private BigDecimal revenue;
    private BigDecimal totalCost;
    private BigDecimal profit;
    private BigDecimal marginPct;
}
