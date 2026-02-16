package com.liquorshop.inventory.service;

import com.liquorshop.inventory.dto.SaleInput;
import com.liquorshop.inventory.dto.SaleItemInput;
import com.liquorshop.inventory.dto.SaleResponse;
import com.liquorshop.inventory.model.Batch;
import com.liquorshop.inventory.model.Sale;
import com.liquorshop.inventory.model.SaleLine;
import com.liquorshop.inventory.repository.BatchRepository;
import com.liquorshop.inventory.repository.SaleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

@Service
@Transactional
@RequiredArgsConstructor
public class SaleService {

    private final SaleRepository saleRepository;
    private final BatchRepository batchRepository;

    public SaleResponse createSale(SaleInput input) {
        // Validate items
        if (input.getItems() == null || input.getItems().isEmpty()) {
            throw new IllegalArgumentException("At least one sale item is required");
        }

        // Validate each item
        for (SaleItemInput item : input.getItems()) {
            if (item.getProductId() == null || item.getQuantity() == null || item.getQuantity() <= 0) {
                throw new IllegalArgumentException("Each item must include product_id and positive quantity");
            }
        }

        // Create sale with initial total_amount = 0
        Sale sale = new Sale();
        sale.setSaleDate(input.getSaleDate() != null ? input.getSaleDate() : LocalDateTime.now());
        sale.setTotalAmount(BigDecimal.ZERO);
        
        // Save sale first to get ID
        sale = saleRepository.save(sale);

        BigDecimal totalAmount = BigDecimal.ZERO;

        // Process each item
        for (SaleItemInput itemInput : input.getItems()) {
            Long productId = itemInput.getProductId();
            Integer requestedQuantity = itemInput.getQuantity();

            // Get batches for this product ordered by expiry_date (nulls last), then created_at
            // SQL equivalent: ORDER BY CASE WHEN expiry_date IS NULL THEN 1 ELSE 0 END, expiry_date, created_at
            List<Batch> batches = batchRepository.findByProductId(productId).stream()
                .filter(b -> b.getCurrentQuantity() > 0)
                .sorted(Comparator
                    // First: batches with expiry_date (false) come before batches without expiry_date (true)
                    .comparing((Batch b) -> b.getExpiryDate() == null)
                    // Then: for batches with expiry_date, order by expiry_date ASC
                    .thenComparing((Batch b) -> b.getExpiryDate() != null ? b.getExpiryDate() : "zzzzzzzzzz",
                                  Comparator.naturalOrder())
                    // Finally: order by created_at
                    .thenComparing(Batch::getCreatedAt))
                .collect(Collectors.toList());

            Integer remaining = requestedQuantity;

            // Allocate from batches (FIFO)
            for (Batch batch : batches) {
                if (remaining <= 0) break;

                Integer available = batch.getCurrentQuantity();
                if (available <= 0) continue;

                Integer take = Math.min(remaining, available);
                
                // Use provided unit_price or batch selling_price
                BigDecimal unitPrice = itemInput.getUnitPrice() != null 
                    ? itemInput.getUnitPrice() 
                    : batch.getSellingPrice();

                // Create sale line
                SaleLine saleLine = new SaleLine();
                saleLine.setSale(sale);
                saleLine.setBatch(batch);
                saleLine.setQuantity(take);
                saleLine.setUnitPrice(unitPrice);
                
                sale.addSaleLine(saleLine);

                // Update batch quantity
                batch.setCurrentQuantity(available - take);
                batchRepository.save(batch);

                // Add to total
                totalAmount = totalAmount.add(unitPrice.multiply(BigDecimal.valueOf(take)));
                remaining -= take;
            }

            // Check if we have enough stock
            if (remaining > 0) {
                throw new IllegalArgumentException("Not enough stock for product_id " + productId);
            }
        }

        // Update sale total amount
        sale.setTotalAmount(totalAmount);
        sale = saleRepository.save(sale);

        // Fetch sale with lines for response
        Sale savedSale = saleRepository.findByIdWithLines(sale.getId())
            .orElseThrow(() -> new RuntimeException("Failed to retrieve created sale"));

        return convertToResponse(savedSale);
    }

    private SaleResponse convertToResponse(Sale sale) {
        SaleResponse response = new SaleResponse();
        response.setId(sale.getId());
        response.setSaleDate(sale.getSaleDate());
        response.setTotalAmount(sale.getTotalAmount());

        List<SaleResponse.SaleLineInfo> lines = sale.getSaleLines().stream()
            .map(sl -> {
                SaleResponse.SaleLineInfo lineInfo = new SaleResponse.SaleLineInfo();
                lineInfo.setId(sl.getId());
                lineInfo.setBatchId(sl.getBatch().getId());
                lineInfo.setQuantity(sl.getQuantity());
                lineInfo.setUnitPrice(sl.getUnitPrice());
                return lineInfo;
            })
            .collect(Collectors.toList());

        response.setLines(lines);
        return response;
    }
}
