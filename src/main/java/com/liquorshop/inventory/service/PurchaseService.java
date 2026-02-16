package com.liquorshop.inventory.service;

import com.liquorshop.inventory.dto.PurchaseInput;
import com.liquorshop.inventory.dto.PurchaseLineInput;
import com.liquorshop.inventory.dto.PurchaseResponse;
import com.liquorshop.inventory.model.Batch;
import com.liquorshop.inventory.model.Product;
import com.liquorshop.inventory.model.Purchase;
import com.liquorshop.inventory.model.PurchaseLine;
import com.liquorshop.inventory.repository.BatchRepository;
import com.liquorshop.inventory.repository.ProductRepository;
import com.liquorshop.inventory.repository.PurchaseRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

@Service
@Transactional
@RequiredArgsConstructor
public class PurchaseService {

    private final PurchaseRepository purchaseRepository;
    private final ProductRepository productRepository;
    private final BatchRepository batchRepository;

    public PurchaseResponse createPurchase(PurchaseInput input) {
        // Validate lines
        if (input.getLines() == null || input.getLines().isEmpty()) {
            throw new IllegalArgumentException("At least one line item is required");
        }

        // Validate each line
        for (PurchaseLineInput line : input.getLines()) {
            if (line.getProductId() == null || line.getQuantity() == null || 
                line.getPurchasePrice() == null || line.getSellingPrice() == null) {
                throw new IllegalArgumentException(
                    "Each line must include product_id, quantity, purchase_price, and selling_price"
                );
            }
        }

        // Create purchase
        Purchase purchase = new Purchase();
        purchase.setSupplierName(input.getSupplierName());
        purchase.setInvoiceNumber(input.getInvoiceNumber());
        purchase.setPurchaseDate(input.getPurchaseDate() != null ? input.getPurchaseDate() : LocalDate.now());
        
        // Save purchase first to get ID
        purchase = purchaseRepository.save(purchase);

        // Process each line item
        for (PurchaseLineInput lineInput : input.getLines()) {
            // Fetch product
            Product product = productRepository.findById(lineInput.getProductId())
                .orElseThrow(() -> new IllegalArgumentException("Product not found: " + lineInput.getProductId()));

            // Create batch
            Batch batch = new Batch();
            batch.setProduct(product);
            batch.setBatchCode(lineInput.getBatchCode());
            batch.setExpiryDate(lineInput.getExpiryDate());
            batch.setPurchasePrice(lineInput.getPurchasePrice());
            batch.setSellingPrice(lineInput.getSellingPrice());
            batch.setCurrentQuantity(lineInput.getQuantity());
            batch.setLocation(lineInput.getLocation());
            
            batch = batchRepository.save(batch);

            // Create purchase line
            PurchaseLine purchaseLine = new PurchaseLine();
            purchaseLine.setPurchase(purchase);
            purchaseLine.setBatch(batch);
            purchaseLine.setQuantity(lineInput.getQuantity());
            
            purchase.addPurchaseLine(purchaseLine);
        }

        // Save purchase with all lines
        purchase = purchaseRepository.save(purchase);

        // Fetch purchase with batches for response
        Purchase savedPurchase = purchaseRepository.findByIdWithBatches(purchase.getId())
            .orElseThrow(() -> new RuntimeException("Failed to retrieve created purchase"));

        return convertToResponse(savedPurchase);
    }

    private PurchaseResponse convertToResponse(Purchase purchase) {
        PurchaseResponse response = new PurchaseResponse();
        response.setId(purchase.getId());
        response.setSupplierName(purchase.getSupplierName());
        response.setInvoiceNumber(purchase.getInvoiceNumber());
        response.setPurchaseDate(purchase.getPurchaseDate());

        List<PurchaseResponse.BatchInfo> batches = purchase.getPurchaseLines().stream()
            .map(pl -> {
                Batch b = pl.getBatch();
                PurchaseResponse.BatchInfo batchInfo = new PurchaseResponse.BatchInfo();
                batchInfo.setId(b.getId());
                batchInfo.setProductId(b.getProduct().getId());
                batchInfo.setBatchCode(b.getBatchCode());
                batchInfo.setExpiryDate(b.getExpiryDate());
                batchInfo.setPurchasePrice(b.getPurchasePrice());
                batchInfo.setSellingPrice(b.getSellingPrice());
                batchInfo.setCurrentQuantity(b.getCurrentQuantity());
                batchInfo.setLocation(b.getLocation());
                return batchInfo;
            })
            .collect(Collectors.toList());

        response.setBatches(batches);
        return response;
    }
}
