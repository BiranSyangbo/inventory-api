package com.liquorshop.inventory.controller;

import com.liquorshop.inventory.dto.PurchaseInput;
import com.liquorshop.inventory.dto.PurchaseResponse;
import com.liquorshop.inventory.service.PurchaseService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/purchases")
@RequiredArgsConstructor
public class PurchaseController {

    private final PurchaseService purchaseService;

    @PostMapping
    public ResponseEntity<?> createPurchase(@Valid @RequestBody PurchaseInput input) {
        try {
            if (input.getLines() == null || input.getLines().isEmpty()) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(Map.of("error", "At least one line item is required"));
            }

            PurchaseResponse purchase = purchaseService.createPurchase(input);
            return ResponseEntity.status(HttpStatus.CREATED).body(purchase);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("error", e.getMessage() != null ? e.getMessage() : "Failed to create purchase"));
        }
    }
}
