package com.liquorshop.inventory.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
public class SaleInput {

    // Null for walk-in; when set, unit prices auto-filled from customer price template
    private Long customerId;

    // Optional: if set, used as-is instead of auto-generating (e.g. bulk import of historical data)
    private String invoiceNumber;

    private LocalDateTime saleDate;

    private BigDecimal discount = BigDecimal.ZERO;

    private BigDecimal vatAmount = BigDecimal.ZERO;

    // PAID | PARTIAL | CREDIT  (default PAID)
    private String paymentStatus = "PAID";

    private String notes;

    @NotEmpty(message = "At least one item is required")
    @Valid
    private List<SaleItemInput> items;
}
