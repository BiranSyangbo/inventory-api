package com.liquorshop.inventory.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class SupplierRequest {

    @NotBlank(message = "Supplier name is required")
    private String name;

    private String contactPerson;
    private String phone;
    private String address;
    private String vatPanNumber;
    private String status = "ACTIVE";
}
