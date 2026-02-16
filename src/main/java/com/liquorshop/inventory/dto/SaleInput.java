package com.liquorshop.inventory.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class SaleInput {

    private LocalDateTime saleDate;

    @NotEmpty(message = "At least one sale item is required")
    @Valid
    private List<SaleItemInput> items;
}
