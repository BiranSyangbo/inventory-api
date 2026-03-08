package com.liquorshop.inventory.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "batches")
@Getter
@Setter
@NoArgsConstructor
public class BatchEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotNull(message = "Product is required")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", nullable = false)
    private ProductEntity product;

    @Column(name = "batch_code")
    private String batchCode;

    @Column(name = "purchase_date", nullable = false)
    private LocalDate purchaseDate;

    // Nullable — only set when product has an expiry date
    @Column(name = "expiry_date")
    private LocalDate expiryDate;

    // Cost per unit at time of purchase; kept for reference and history
    @NotNull
    @Min(value = 0, message = "Purchase price cannot be negative")
    @Column(name = "purchase_price", nullable = false, precision = 10, scale = 2)
    private BigDecimal purchasePrice;

    @NotNull
    @Min(value = 0)
    @Column(name = "original_quantity", nullable = false)
    private Integer originalQuantity;

    // Decremented on every sale that draws from this batch
    @NotNull
    @Min(value = 0, message = "Current quantity cannot be negative")
    @Column(name = "current_quantity", nullable = false)
    private Integer currentQuantity;

    @Column(name = "location")
    private String location;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        if (purchaseDate == null) {
            purchaseDate = LocalDate.now();
        }
    }
}
