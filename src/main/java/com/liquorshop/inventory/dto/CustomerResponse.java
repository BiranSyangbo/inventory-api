package com.liquorshop.inventory.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class CustomerResponse {

    private Long id;
    private String name;
    private String phone;
    private String address;
    private BigDecimal creditLimit;
    private BigDecimal outstandingBalance;
    private LocalDateTime createdAt;
}
