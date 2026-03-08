package com.liquorshop.inventory.controller;

import com.liquorshop.inventory.dto.PaymentRequest;
import com.liquorshop.inventory.dto.PaymentResponse;
import com.liquorshop.inventory.dto.SaleInput;
import com.liquorshop.inventory.dto.SaleResponse;
import com.liquorshop.inventory.service.SaleService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/sales")
@RequiredArgsConstructor
public class SaleController {

    private final SaleService saleService;

    @GetMapping
    public ResponseEntity<List<SaleResponse>> getAll() {
        return ResponseEntity.ok(saleService.getAll());
    }

    @GetMapping("/{id}")
    public ResponseEntity<SaleResponse> getById(@PathVariable Long id) {
        return ResponseEntity.ok(saleService.getById(id));
    }

    @PostMapping
    public ResponseEntity<SaleResponse> create(@Valid @RequestBody SaleInput input) {
        return ResponseEntity.status(HttpStatus.CREATED).body(saleService.create(input));
    }

    @PostMapping("/{id}/payments")
    public ResponseEntity<PaymentResponse> addPayment(@PathVariable Long id,
                                                       @Valid @RequestBody PaymentRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(saleService.addPayment(id, request));
    }

    @GetMapping("/{id}/payments")
    public ResponseEntity<List<PaymentResponse>> getPayments(@PathVariable Long id) {
        return ResponseEntity.ok(saleService.getPayments(id));
    }
}
