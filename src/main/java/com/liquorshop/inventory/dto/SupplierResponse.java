package com.liquorshop.inventory.dto;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class SupplierResponse {

    private Long id;
    private String name;
    private String contactPerson;
    private String phone;
    private String address;
    private String vatPanNumber;
    private String status;
    private LocalDateTime createdAt;
}
