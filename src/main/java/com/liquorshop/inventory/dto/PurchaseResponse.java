package com.liquorshop.inventory.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Data
public class PurchaseResponse {

    private Long id;
    private Long supplierId;
    private String supplierName;
    private String vatBillNumber;
    private LocalDate purchaseDate;
    private BigDecimal invoiceAmount;
    private BigDecimal vatAmount;
    private BigDecimal discount;
    private String remarks;
    private LocalDateTime createdAt;
    private List<PurchaseLineResponse> lines;
    private BigDecimal totalPaid;
    private BigDecimal outstandingAmount;

    @Data
    public static class PurchaseLineResponse {
        private Long id;
        private Long productId;
        private String productName;
        private Long batchId;
        private String batchCode;
        private LocalDate expiryDate;
        private Integer quantity;
        private BigDecimal purchasePrice;
        private BigDecimal vatPercent;
        private String location;
    }
}
