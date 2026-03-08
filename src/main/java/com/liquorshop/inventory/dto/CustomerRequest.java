package com.liquorshop.inventory.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class CustomerRequest {

    @NotBlank(message = "Customer name is required")
    private String name;

    private String phone;
    private String address;
    private BigDecimal creditLimit = BigDecimal.ZERO;
}
