package com.liquorshop.inventory.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
public class SaleResponse {

    private Long id;
    private Long customerId;
    private String customerName;
    private String invoiceNumber;
    private LocalDateTime saleDate;
    private BigDecimal totalAmount;
    private BigDecimal discount;
    private BigDecimal vatAmount;
    private String paymentStatus;
    private String notes;
    private LocalDateTime createdAt;
    private List<SaleLineResponse> lines;
    private BigDecimal totalPaid;
    private BigDecimal outstandingAmount;

    @Data
    public static class SaleLineResponse {
        private Long id;
        private Long productId;
        private String productName;
        private Long batchId;
        private String batchCode;
        private Integer quantity;
        private BigDecimal unitPrice;
        private BigDecimal costPriceAtSale;
        private BigDecimal lineTotal;
        private BigDecimal profit; // (unitPrice - costPriceAtSale) * quantity
    }
}
