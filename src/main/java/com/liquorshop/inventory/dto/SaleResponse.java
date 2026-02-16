package com.liquorshop.inventory.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class SaleResponse {

    private Long id;
    private LocalDateTime saleDate;
    private BigDecimal totalAmount;
    private List<SaleLineInfo> lines;

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SaleLineInfo {
        private Long id;
        private Long batchId;
        private Integer quantity;
        private BigDecimal unitPrice;
    }
}
