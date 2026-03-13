package com.liquorshop.inventory.dto;

import jakarta.persistence.Column;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class ProductResponse {

    private Long id;
    private String name;
    private String brand;
    private String category;
    private String volumeMl;
    private String barcode;
    private Integer minStock;
    private BigDecimal sellingPrice;
    private BigDecimal averageCost;
    private String status;
    private LocalDateTime createdAt;
    // Current total stock across all batches — populated by InventoryService
    private Integer currentStock;
    private String type;
    private BigDecimal alcoholPercentage;
    private String mrp;
}
