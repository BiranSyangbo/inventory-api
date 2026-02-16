package com.liquorshop.inventory.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PurchaseInput {

    private String supplierName;

    private String invoiceNumber;

    private LocalDate purchaseDate;

    @NotEmpty(message = "At least one line item is required")
    @Valid
    private List<PurchaseLineInput> lines;
}
