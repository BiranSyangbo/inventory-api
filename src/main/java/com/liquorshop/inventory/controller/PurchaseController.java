package com.liquorshop.inventory.controller;

import com.liquorshop.inventory.dto.PaymentRequest;
import com.liquorshop.inventory.dto.PaymentResponse;
import com.liquorshop.inventory.dto.PurchaseInput;
import com.liquorshop.inventory.dto.PurchaseResponse;
import com.liquorshop.inventory.service.PurchaseService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/purchases")
@RequiredArgsConstructor
public class PurchaseController {

    private final PurchaseService purchaseService;

    @GetMapping
    public ResponseEntity<List<PurchaseResponse>> getAll() {
        return ResponseEntity.ok(purchaseService.getAll());
    }

    @GetMapping("/{id}")
    public ResponseEntity<PurchaseResponse> getById(@PathVariable Long id) {
        return ResponseEntity.ok(purchaseService.getById(id));
    }

    @PostMapping
    public ResponseEntity<PurchaseResponse> create(@Valid @RequestBody PurchaseInput input) {
        return ResponseEntity.status(HttpStatus.CREATED).body(purchaseService.create(input));
    }

    @PostMapping("/{id}/payments")
    public ResponseEntity<PaymentResponse> addPayment(@PathVariable Long id,
                                                       @Valid @RequestBody PaymentRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(purchaseService.addPayment(id, request));
    }

    @GetMapping("/{id}/payments")
    public ResponseEntity<List<PaymentResponse>> getPayments(@PathVariable Long id) {
        return ResponseEntity.ok(purchaseService.getPayments(id));
    }
}
