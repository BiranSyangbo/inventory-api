package com.liquorshop.inventory.controller;

import com.liquorshop.inventory.dto.CurrentInventoryResponse;
import com.liquorshop.inventory.dto.ExpiringBatchResponse;
import com.liquorshop.inventory.dto.LowStockResponse;
import com.liquorshop.inventory.service.InventoryService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/inventory")
@RequiredArgsConstructor
public class InventoryController {

    private final InventoryService inventoryService;

    @GetMapping("/current")
    public ResponseEntity<List<CurrentInventoryResponse>> getCurrentInventory() {
        List<CurrentInventoryResponse> inventory = inventoryService.getCurrentInventory();
        return ResponseEntity.ok(inventory);
    }

    @GetMapping("/low")
    public ResponseEntity<List<LowStockResponse>> getLowStock() {
        List<LowStockResponse> lowStock = inventoryService.getLowStock();
        return ResponseEntity.ok(lowStock);
    }

    @GetMapping("/expiring")
    public ResponseEntity<List<ExpiringBatchResponse>> getExpiringBatches(
            @RequestParam(required = false) Integer days) {
        List<ExpiringBatchResponse> expiringBatches = inventoryService.getExpiringBatches(days);
        return ResponseEntity.ok(expiringBatches);
    }
}
