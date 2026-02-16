package com.liquorshop.inventory.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Min;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "products")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Product {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank(message = "Product name is required")
    @Column(nullable = false)
    private String name;

    @Column(name = "category")
    private String category;

    @Column(name = "brand")
    private String brand;

    @Column(name = "volume_ml")
    private Integer volumeMl;

    @Column(name = "unit")
    private String unit;

    @Column(name = "barcode", unique = true)
    private String barcode;

    @Min(value = 0, message = "Minimum stock cannot be negative")
    @Column(name = "min_stock", nullable = false)
    private Integer minStock = 0;

    public void setMinStock(Integer minStock) {
        this.minStock = minStock != null ? minStock : 0;
    }
}
