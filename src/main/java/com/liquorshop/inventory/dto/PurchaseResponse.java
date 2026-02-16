package com.liquorshop.inventory.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PurchaseResponse {

    private Long id;
    private String supplierName;
    private String invoiceNumber;
    private LocalDate purchaseDate;
    private List<BatchInfo> batches;

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class BatchInfo {
        private Long id;
        private Long productId;
        private String batchCode;
        private String expiryDate;
        private BigDecimal purchasePrice;
        private BigDecimal sellingPrice;
        private Integer currentQuantity;
        private String location;
    }
}
