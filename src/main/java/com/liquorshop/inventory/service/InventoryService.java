package com.liquorshop.inventory.service;

import com.liquorshop.inventory.dto.CurrentInventoryResponse;
import com.liquorshop.inventory.dto.ExpiringBatchResponse;
import com.liquorshop.inventory.dto.LowStockResponse;
import com.liquorshop.inventory.repository.BatchRepository;
import com.liquorshop.inventory.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class InventoryService {

    private final ProductRepository productRepository;
    private final BatchRepository batchRepository;

    public List<CurrentInventoryResponse> getCurrentInventory() {
        return productRepository.findAllByDeletedFalseOrderByNameAsc()
                .stream()
                .map(product -> {
                    int totalQty = product.getCurrentStock();
                    BigDecimal stockValue = product.getAverageCost()
                            .multiply(BigDecimal.valueOf(totalQty));

                    CurrentInventoryResponse r = new CurrentInventoryResponse();
                    r.setProductId(product.getId());
                    r.setName(product.getName());
                    r.setBrand(product.getBrand());
                    r.setCategory(product.getCategory());
                    r.setVolumeMl(product.getVolumeMl());
                    r.setMinStock(product.getMinStock());
                    r.setTotalQuantity(totalQty);
                    r.setAverageCost(product.getAverageCost());
                    r.setSellingPrice(product.getSellingPrice());
                    r.setTotalValue(stockValue);
                    r.setIsLowStock(totalQty < product.getMinStock());
                    return r;
                })
                .collect(Collectors.toList());
    }

    public List<LowStockResponse> getLowStock() {
        return productRepository.findAllByDeletedFalseOrderByNameAsc()
                .stream()
                .map(product -> {
                    int totalQty = product.getCurrentStock();
                    if (totalQty >= product.getMinStock()) return null;

                    LowStockResponse r = new LowStockResponse();
                    r.setProductId(product.getId());
                    r.setName(product.getName());
                    r.setBrand(product.getBrand());
                    r.setCategory(product.getCategory());
                    r.setVolumeMl(product.getVolumeMl());
                    r.setMinStock(product.getMinStock());
                    r.setTotalQuantity(totalQty);
                    return r;
                })
                .filter(Objects::nonNull)
                .sorted(Comparator.comparingInt(LowStockResponse::getTotalQuantity))
                .collect(Collectors.toList());
    }

    public List<ExpiringBatchResponse> getExpiringBatches(Integer days) {
        int window = (days != null && days > 0) ? days : 30;
        LocalDate cutoff = LocalDate.now().plusDays(window);
        LocalDate today = LocalDate.now();

        return batchRepository.findExpiringWithStock(cutoff)
                .stream()
                .map(batch -> {
                    String status = batch.getExpiryDate().isBefore(today) ? "expired" : "expiring_soon";
                    ExpiringBatchResponse r = new ExpiringBatchResponse();
                    r.setId(batch.getId());
                    r.setProductId(batch.getProduct().getId());
                    r.setProductName(batch.getProduct().getName());
                    r.setProductBrand(batch.getProduct().getBrand());
                    r.setBatchCode(batch.getBatchCode());
                    r.setExpiryDate(batch.getExpiryDate());
                    r.setPurchasePrice(batch.getPurchasePrice());
                    r.setCurrentQuantity(batch.getCurrentQuantity());
                    r.setLocation(batch.getLocation());
                    r.setCreatedAt(batch.getCreatedAt());
                    r.setStatus(status);
                    return r;
                })
                .collect(Collectors.toList());
    }
}
