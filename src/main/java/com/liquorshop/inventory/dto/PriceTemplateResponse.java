package com.liquorshop.inventory.dto;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class PriceTemplateResponse {

    private Long id;
    private Long customerId;
    private Long productId;
    private String productName;
    private String productBrand;
    private String productVolumeMl;
    private BigDecimal sellingPrice;
    private BigDecimal standardPrice; // product.sellingPrice for comparison
}
