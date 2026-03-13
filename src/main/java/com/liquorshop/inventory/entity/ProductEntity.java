package com.liquorshop.inventory.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "products")
@Getter
@Setter
@NoArgsConstructor
public class ProductEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank(message = "Product name is required")
    @Column(nullable = false)
    private String name;

    @Column(name = "brand")
    private String brand;

    @Column(name = "category")
    private String category;

    @Column(name = "volume_ml")
    private String volumeMl;

    @Column(name = "type", comment = "Contains details like a Full, Half or Quarter")
    private String type;

    @Column(name = "percentage", comment = "Alcohol percentage")
    private BigDecimal alcoholPercentage;

    @Column(name = "current_stock", comment = "Remaining Stock")
    private int currentStock = 0;

    @Column(name = "mrp", comment = "Maximum Retail Price")
    private String mrp;

    @Column(name = "barcode", unique = true)
    private String barcode;

    @Min(value = 0, message = "Minimum stock cannot be negative")
    @Column(name = "min_stock", nullable = false)
    private Integer minStock = 0;

    @NotNull
    @Column(name = "selling_price", nullable = false, precision = 10, scale = 2)
    private BigDecimal sellingPrice = BigDecimal.ZERO;

    // Weighted average cost — recalculated on every purchase line for this product
    @Column(name = "average_cost", nullable = false, precision = 10, scale = 2)
    private BigDecimal averageCost = BigDecimal.ZERO;

    // ACTIVE | INACTIVE
    @Column(name = "status", nullable = false)
    private String status = "ACTIVE";

    // Soft delete — deleted = true means hidden from frontend
    @Column(name = "deleted", nullable = false)
    private Boolean deleted = false;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
}
