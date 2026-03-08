package com.liquorshop.inventory.controller;

import com.liquorshop.inventory.dto.CustomerRequest;
import com.liquorshop.inventory.dto.CustomerResponse;
import com.liquorshop.inventory.dto.CustomerStatementResponse;
import com.liquorshop.inventory.dto.PaymentResponse;
import com.liquorshop.inventory.dto.PriceTemplateRequest;
import com.liquorshop.inventory.dto.PriceTemplateResponse;
import com.liquorshop.inventory.service.CustomerService;
import com.liquorshop.inventory.service.SaleService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/customers")
@RequiredArgsConstructor
public class CustomerController {

    private final CustomerService customerService;
    private final SaleService saleService;

    @GetMapping
    public ResponseEntity<List<CustomerResponse>> getAll() {
        return ResponseEntity.ok(customerService.getAll());
    }

    @GetMapping("/{id}")
    public ResponseEntity<CustomerResponse> getById(@PathVariable Long id) {
        return ResponseEntity.ok(customerService.getById(id));
    }

    @PostMapping
    public ResponseEntity<CustomerResponse> create(@Valid @RequestBody CustomerRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(customerService.create(request));
    }

    @PutMapping("/{id}")
    public ResponseEntity<CustomerResponse> update(@PathVariable Long id,
                                                    @Valid @RequestBody CustomerRequest request) {
        return ResponseEntity.ok(customerService.update(id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        customerService.delete(id);
        return ResponseEntity.noContent().build();
    }

    // ── Price Template ──────────────────────────────────────────────────────

    @GetMapping("/{id}/price-template")
    public ResponseEntity<List<PriceTemplateResponse>> getTemplate(@PathVariable Long id) {
        return ResponseEntity.ok(customerService.getTemplate(id));
    }

    @PostMapping("/{id}/price-template")
    public ResponseEntity<PriceTemplateResponse> upsertTemplateEntry(
            @PathVariable Long id,
            @Valid @RequestBody PriceTemplateRequest request) {
        return ResponseEntity.ok(customerService.upsertTemplateEntry(id, request));
    }

    @DeleteMapping("/{id}/price-template/{productId}")
    public ResponseEntity<Void> deleteTemplateEntry(@PathVariable Long id,
                                                     @PathVariable Long productId) {
        customerService.deleteTemplateEntry(id, productId);
        return ResponseEntity.noContent().build();
    }

    // ── Statement & Payment History ─────────────────────────────────────────

    @GetMapping("/{id}/statement")
    public ResponseEntity<CustomerStatementResponse> getStatement(@PathVariable Long id) {
        return ResponseEntity.ok(customerService.getStatement(id));
    }

    @GetMapping("/{id}/payments")
    public ResponseEntity<List<PaymentResponse>> getPayments(@PathVariable Long id) {
        return ResponseEntity.ok(saleService.getCustomerPayments(id));
    }
}
