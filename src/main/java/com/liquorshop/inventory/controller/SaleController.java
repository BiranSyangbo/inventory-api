package com.liquorshop.inventory.controller;

import com.liquorshop.inventory.dto.SaleInput;
import com.liquorshop.inventory.dto.SaleResponse;
import com.liquorshop.inventory.service.SaleService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/sales")
@RequiredArgsConstructor
public class SaleController {

    private final SaleService saleService;

    @PostMapping
    public ResponseEntity<?> createSale(@Valid @RequestBody SaleInput input) {
        try {
            if (input.getItems() == null || input.getItems().isEmpty()) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(Map.of("error", "At least one sale item is required"));
            }

            SaleResponse sale = saleService.createSale(input);
            return ResponseEntity.status(HttpStatus.CREATED).body(sale);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("error", e.getMessage() != null ? e.getMessage() : "Failed to create sale"));
        }
    }
}
