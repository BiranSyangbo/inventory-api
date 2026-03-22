package com.liquorshop.inventory.dto;

import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@NoArgsConstructor
public class VatSalesLine {
    private Long saleId;
    private LocalDate saleDate;
    private String invoiceNumber;
    private BigDecimal totalAmount;
    private BigDecimal vatAmount;
}
