package com.liquorshop.inventory.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "purchases")
@Getter
@Setter
@NoArgsConstructor
public class Purchase {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "supplier_name")
    private String supplierName;

    @Column(name = "invoice_number")
    private String invoiceNumber;

    @Column(name = "purchase_date", nullable = false)
    private LocalDate purchaseDate;

    @OneToMany(mappedBy = "purchase", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<PurchaseLine> purchaseLines = new ArrayList<>();

    @PrePersist
    protected void onCreate() {
        if (purchaseDate == null) {
            purchaseDate = LocalDate.now();
        }
    }

    public void addPurchaseLine(PurchaseLine purchaseLine) {
        purchaseLines.add(purchaseLine);
        purchaseLine.setPurchase(this);
    }

    public void removePurchaseLine(PurchaseLine purchaseLine) {
        purchaseLines.remove(purchaseLine);
        purchaseLine.setPurchase(null);
    }
}
