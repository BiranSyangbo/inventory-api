package com.liquorshop.inventory.service;

import com.liquorshop.inventory.dto.CurrentInventoryResponse;
import com.liquorshop.inventory.dto.ExpiringBatchResponse;
import com.liquorshop.inventory.dto.LowStockResponse;
import com.liquorshop.inventory.model.Batch;
import com.liquorshop.inventory.model.Product;
import com.liquorshop.inventory.repository.BatchRepository;
import com.liquorshop.inventory.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class InventoryService {

    private final ProductRepository productRepository;
    private final BatchRepository batchRepository;

    public List<CurrentInventoryResponse> getCurrentInventory() {
        List<Product> products = productRepository.findAllByOrderByNameAsc();

        return products.stream()
            .map(product -> {
                List<Batch> batches = batchRepository.findByProductId(product.getId());
                
                Integer totalQuantity = batches.stream()
                    .mapToInt(Batch::getCurrentQuantity)
                    .sum();

                BigDecimal totalValue = batches.stream()
                    .map(b -> b.getSellingPrice().multiply(BigDecimal.valueOf(b.getCurrentQuantity())))
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

                Integer minStock = product.getMinStock() != null ? product.getMinStock() : 0;
                Boolean isLowStock = totalQuantity < minStock;

                CurrentInventoryResponse response = new CurrentInventoryResponse();
                response.setProductId(product.getId());
                response.setName(product.getName());
                response.setBrand(product.getBrand());
                response.setCategory(product.getCategory());
                response.setVolumeMl(product.getVolumeMl());
                response.setUnit(product.getUnit());
                response.setMinStock(minStock);
                response.setTotalQuantity(totalQuantity);
                response.setTotalValue(totalValue);
                response.setIsLowStock(isLowStock);

                return response;
            })
            .collect(Collectors.toList());
    }

    public List<LowStockResponse> getLowStock() {
        List<Product> products = productRepository.findAllByOrderByNameAsc();

        return products.stream()
            .map(product -> {
                List<Batch> batches = batchRepository.findByProductId(product.getId());
                
                Integer totalQuantity = batches.stream()
                    .mapToInt(Batch::getCurrentQuantity)
                    .sum();

                Integer minStock = product.getMinStock() != null ? product.getMinStock() : 0;

                if (totalQuantity >= minStock) {
                    return null; // Filter out products that are not low stock
                }

                LowStockResponse response = new LowStockResponse();
                response.setProductId(product.getId());
                response.setName(product.getName());
                response.setBrand(product.getBrand());
                response.setCategory(product.getCategory());
                response.setVolumeMl(product.getVolumeMl());
                response.setUnit(product.getUnit());
                response.setMinStock(minStock);
                response.setTotalQuantity(totalQuantity);

                return response;
            })
            .filter(response -> response != null)
            .sorted((a, b) -> Integer.compare(a.getTotalQuantity(), b.getTotalQuantity()))
            .collect(Collectors.toList());
    }

    public List<ExpiringBatchResponse> getExpiringBatches(Integer days) {
        if (days == null || days <= 0) {
            days = 30; // Default to 30 days
        }

        LocalDate cutoffDate = LocalDate.now().plusDays(days);
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");

        List<Batch> batches = batchRepository.findAll().stream()
            .filter(batch -> 
                batch.getExpiryDate() != null && 
                !batch.getExpiryDate().isEmpty() &&
                batch.getCurrentQuantity() > 0
            )
            .filter(batch -> {
                try {
                    LocalDate expiryDate = LocalDate.parse(batch.getExpiryDate(), formatter);
                    return expiryDate.isBefore(cutoffDate) || expiryDate.isEqual(cutoffDate);
                } catch (Exception e) {
                    return false; // Skip batches with invalid date format
                }
            })
            .sorted((a, b) -> {
                try {
                    LocalDate dateA = LocalDate.parse(a.getExpiryDate(), formatter);
                    LocalDate dateB = LocalDate.parse(b.getExpiryDate(), formatter);
                    return dateA.compareTo(dateB);
                } catch (Exception e) {
                    return 0;
                }
            })
            .collect(Collectors.toList());

        LocalDate now = LocalDate.now();

        return batches.stream()
            .map(batch -> {
                String status = "expiring_soon";
                try {
                    LocalDate expiryDate = LocalDate.parse(batch.getExpiryDate(), formatter);
                    if (expiryDate.isBefore(now)) {
                        status = "expired";
                    }
                } catch (Exception e) {
                    // Keep default status
                }

                ExpiringBatchResponse response = new ExpiringBatchResponse();
                response.setId(batch.getId());
                response.setProductId(batch.getProduct().getId());
                response.setBatchCode(batch.getBatchCode());
                response.setExpiryDate(batch.getExpiryDate());
                response.setPurchasePrice(batch.getPurchasePrice());
                response.setSellingPrice(batch.getSellingPrice());
                response.setCurrentQuantity(batch.getCurrentQuantity());
                response.setLocation(batch.getLocation());
                response.setCreatedAt(batch.getCreatedAt());
                response.setProductName(batch.getProduct().getName());
                response.setProductBrand(batch.getProduct().getBrand());
                response.setStatus(status);

                return response;
            })
            .collect(Collectors.toList());
    }
}
