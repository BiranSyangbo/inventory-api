package com.liquorshop.inventory.service.bulk;

import com.liquorshop.inventory.dto.ImportResult;
import com.liquorshop.inventory.dto.PurchaseInput;
import com.liquorshop.inventory.dto.PurchaseLineInput;
import com.liquorshop.inventory.entity.SupplierEntity;
import com.liquorshop.inventory.repository.ProductRepository;
import com.liquorshop.inventory.repository.SupplierRepository;
import com.liquorshop.inventory.service.PurchaseService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.util.*;

import static com.liquorshop.inventory.service.bulk.BulkImportUtils.*;
import static com.liquorshop.inventory.service.bulk.FileHandler.parseFile;

@Service
@RequiredArgsConstructor
public class BulkPurchasesService {

    private final SupplierRepository supplierRepository;

    private final ProductRepository productRepository;
    private final PurchaseService purchaseService;

    public ImportResult importPurchases(MultipartFile file) throws Exception {
        List<String[]> rows = parseFile(file);
        ImportResult result = new ImportResult();
        result.setTotalRows(rows.size());

        // Group rows by vat_bill_number (col 1).
        // Blank vat_bill_number → synthetic key keeps each row as separate purchase.
        Map<String, List<int[]>> groups = new LinkedHashMap<>(); // key → list of row indices
        for (int i = 0; i < rows.size(); i++) {
            String[] r = rows.get(i);
            String vatBill = optional(r, 1);
            String key = (vatBill != null && !vatBill.isBlank()) ? vatBill : ("__row_" + i);
            groups.computeIfAbsent(key, k -> new ArrayList<>()).add(new int[]{i});
        }

        for (Map.Entry<String, List<int[]>> entry : groups.entrySet()) {
            List<int[]> indices = entry.getValue();
            // Use first row of the group for header fields
            int firstIdx = indices.getFirst()[0];
            int firstRowNum = firstIdx + 2;
            try {
                String[] header = rows.get(firstIdx);

                String supplierName = require(header, 0, "supplier_name");
                Optional<SupplierEntity> supplierOpt = supplierRepository.findByNameIgnoreCase(supplierName);
                if (supplierOpt.isEmpty()) {
                    for (int[] idx : indices) {
                        result.addError(idx[0] + 2, "Supplier not found: " + supplierName);
                    }
                    continue;
                }

                PurchaseInput input = new PurchaseInput();
                input.setSupplierId(supplierOpt.get().getId());
                String vatBill = optional(header, 1);
                input.setVatBillNumber(vatBill != null && !vatBill.isBlank() ? vatBill : null);
                input.setPurchaseDate(parseDate(optional(header, 2)));
                input.setInvoiceAmount(optionalDecimal(header, 3));
                input.setVatAmount(optionalDecimalOrZero(header, 4));
                input.setDiscount(optionalDecimalOrZero(header, 5));
                input.setRemarks(optional(header, 6));
                input.setLines(new ArrayList<>());

                for (int[] idx : indices) {
                    int rowNum = idx[0] + 2;
                    try {
                        String[] r = rows.get(idx[0]);
                        String barcode = require(r, 7, "product_barcode");
                        var productOpt = productRepository.findByBarcodeAndDeletedFalse(barcode);
                        if (productOpt.isEmpty()) {
                            result.addError(rowNum, "Product not found by barcode: " + barcode);
                            continue;
                        }

                        PurchaseLineInput line = new PurchaseLineInput();
                        line.setProductId(productOpt.get().getId());
                        line.setQuantity(requireInt(r, 8, "quantity"));
                        line.setPurchasePrice(requireDecimal(r, 9, "purchase_price"));
                        line.setVatPercent(optionalDecimalOrZero(r, 10));
                        line.setExpiryDate(parseDate(optional(r, 11)));
                        input.getLines().add(line);
                    } catch (Exception ex) {
                        result.addError(rowNum, ex.getMessage());
                    }
                }

                if (input.getLines().isEmpty()) {
                    // All lines failed — skip this purchase
                    continue;
                }

                purchaseService.create(input);
                int successRows = input.getLines().size();
                for (int s = 0; s < successRows; s++) result.incrementSuccess();

            } catch (Exception ex) {
                result.addError(firstRowNum, ex.getMessage());
            }
        }
        return result;
    }
}
