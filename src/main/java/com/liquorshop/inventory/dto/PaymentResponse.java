package com.liquorshop.inventory.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class PaymentResponse {

    private Long id;
    private Long referenceId; // purchaseId or saleId
    private Long partyId;     // supplierId or customerId
    private String partyName;
    private LocalDateTime paymentDate;
    private BigDecimal amount;
    private String paymentMethod;
    private String referenceNumber;
    private String notes;
}
