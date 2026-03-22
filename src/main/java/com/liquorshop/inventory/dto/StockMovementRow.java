package com.liquorshop.inventory.dto;

import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@NoArgsConstructor
public class StockMovementRow {
    private LocalDate date;
    /** PURCHASE or SALE */
    private String transactionType;
    private String referenceNumber;
    private int quantityIn;
    private int quantityOut;
    /** Running balance computed in service */
    private int runningBalance;
}
