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
@RequestMapping("/api/inventory")
@RequiredArgsConstructor
public class InventoryController {

    private final InventoryService inventoryService;

    @GetMapping
    public ResponseEntity<List<CurrentInventoryResponse>> getCurrentInventory() {
        return ResponseEntity.ok(inventoryService.getCurrentInventory());
    }

    @GetMapping("/low-stock")
    public ResponseEntity<List<LowStockResponse>> getLowStock() {
        return ResponseEntity.ok(inventoryService.getLowStock());
    }

    @GetMapping("/expiring")
    public ResponseEntity<List<ExpiringBatchResponse>> getExpiringBatches(
            @RequestParam(required = false) Integer days) {
        return ResponseEntity.ok(inventoryService.getExpiringBatches(days));
    }
}
