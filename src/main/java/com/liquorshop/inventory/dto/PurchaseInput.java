package com.liquorshop.inventory.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Data
public class PurchaseInput {

    @NotNull(message = "Supplier ID is required")
    private Long supplierId;

    private String vatBillNumber;

    private LocalDate purchaseDate;

    private BigDecimal invoiceAmount;

    private BigDecimal vatAmount = BigDecimal.ZERO;

    private BigDecimal discount = BigDecimal.ZERO;

    private String remarks;

    @NotEmpty(message = "At least one line item is required")
    @Valid
    private List<PurchaseLineInput> lines;
}
