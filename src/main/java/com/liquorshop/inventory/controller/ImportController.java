package com.liquorshop.inventory.controller;

import com.liquorshop.inventory.dto.ImportResult;
import com.liquorshop.inventory.service.bulk.BulkCustomerService;
import com.liquorshop.inventory.service.bulk.BulkProductService;
import com.liquorshop.inventory.service.bulk.BulkPurchasesService;
import com.liquorshop.inventory.service.bulk.BulkSalesService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

/**
 * Bulk import endpoints — accept CSV or Excel (.xlsx) files.
 * <p>
 * POST /api/import/products   — import product master data
 * POST /api/import/customers  — import customer master data
 * POST /api/import/purchases  — import purchase history (creates batches, updates avg cost)
 * POST /api/import/sales      — import sales history (FIFO batch allocation)
 * <p>
 * All endpoints accept a multipart/form-data request with a single field named "file".
 * They return an ImportResult describing how many rows succeeded and which rows failed with reasons.
 */
@RestController
@RequestMapping("/api/import")
@RequiredArgsConstructor
public class ImportController {

    private final BulkCustomerService bulkCustomerService;
    private final BulkPurchasesService bulkPurchasesService;
    private final BulkSalesService bulkSalesService;
    private final BulkProductService bulkProductService;

    @PostMapping("/products")
    public ResponseEntity<ImportResult> importProducts(@RequestParam("file") MultipartFile file) throws Exception {
        return ResponseEntity.ok(bulkProductService.importProducts(file));
    }

    @PostMapping("/customers")
    public ResponseEntity<ImportResult> importCustomers(@RequestParam("file") MultipartFile file) throws Exception {
        return ResponseEntity.ok(bulkCustomerService.importCustomers(file));
    }

    @PostMapping("/purchases")
    public ResponseEntity<ImportResult> importPurchases(@RequestParam("file") MultipartFile file) throws Exception {
        return ResponseEntity.ok(bulkPurchasesService.importPurchases(file));
    }

    @PostMapping("/sales")
    public ResponseEntity<ImportResult> importSales(@RequestParam("file") MultipartFile file) throws Exception {
        return ResponseEntity.ok(bulkSalesService.importSales(file));
    }
}
