package com.liquorshop.inventory.service.bulk;

import com.liquorshop.inventory.dto.ImportResult;
import com.liquorshop.inventory.dto.ProductRequest;
import com.liquorshop.inventory.service.ProductService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

import static com.liquorshop.inventory.service.bulk.BulkImportUtils.*;
import static com.liquorshop.inventory.service.bulk.FileHandler.parseFile;

@Service
@RequiredArgsConstructor
public class BulkProductService {

    private final ProductService productService;

    public ImportResult importProducts(MultipartFile file) throws Exception {
        List<String[]> rows = parseFile(file);
        ImportResult result = new ImportResult();
        result.setTotalRows(rows.size());

        for (int i = 0; i < rows.size(); i++) {
            int rowNum = i + 2; // 1-indexed, +1 for header
            try {
                String[] r = rows.get(i);
                ProductRequest req = new ProductRequest();
                req.setName(require(r, 0, "name"));
                req.setBrand(optional(r, 1));
                req.setCategory(optional(r, 2));
                req.setVolumeMl(optionalInt(r, 3));
                req.setUnit(optional(r, 4));
                req.setBarcode(optional(r, 5));
                req.setMinStock(optionalInt(r, 6) != null ? optionalInt(r, 6) : 0);
                req.setSellingPrice(requireDecimal(r, 7, "selling_price"));
                String status = optional(r, 8);
                req.setStatus(status != null && !status.isBlank() ? status.toUpperCase() : "ACTIVE");

                productService.create(req);
                result.incrementSuccess();
            } catch (Exception ex) {
                result.addError(rowNum, ex.getMessage());
            }
        }
        return result;
    }


}
