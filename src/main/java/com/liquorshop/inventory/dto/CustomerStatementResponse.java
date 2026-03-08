package com.liquorshop.inventory.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
public class CustomerStatementResponse {

    private Long customerId;
    private String customerName;
    private BigDecimal creditLimit;
    private BigDecimal outstandingBalance;
    private List<StatementEntry> entries;

    @Data
    public static class StatementEntry {
        private LocalDateTime date;
        private String type;           // SALE | PAYMENT
        private String reference;      // invoice number or payment ID
        private String paymentMethod;  // null for SALE entries
        private String referenceNumber;// cheque/txn ID for payments
        private BigDecimal debit;      // amount added to outstanding (SALE)
        private BigDecimal credit;     // amount reducing outstanding (PAYMENT)
        private BigDecimal balance;    // running balance
    }
}
