package com.liquorshop.inventory.service.bulk;

import com.liquorshop.inventory.dto.ImportResult;
import com.liquorshop.inventory.dto.SaleInput;
import com.liquorshop.inventory.dto.SaleItemInput;
import com.liquorshop.inventory.entity.CustomerEntity;
import com.liquorshop.inventory.repository.CustomerRepository;
import com.liquorshop.inventory.repository.ProductRepository;
import com.liquorshop.inventory.service.SaleService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.util.*;

import static com.liquorshop.inventory.service.bulk.BulkImportUtils.*;
import static com.liquorshop.inventory.service.bulk.FileHandler.parseFile;

@Service
@RequiredArgsConstructor
public class BulkSalesService {

    private final SaleService saleService;
    private final ProductRepository productRepository;
    private final CustomerRepository customerRepository;

    public ImportResult importSales(MultipartFile file) throws Exception {

        List<String[]> rows = parseFile(file);
        ImportResult result = new ImportResult();
        result.setTotalRows(rows.size());
        Map<String, List<Integer>> groups = groupRowsByInvoice(rows);

        for (Map.Entry<String, List<Integer>> entry : groups.entrySet()) {
            List<Integer> indices = entry.getValue();
            int firstIdx = indices.stream().findFirst().orElseThrow();
            int firstRowNum = firstIdx + 2;
            try {

                SaleInput input = mapSaleHeader(rows.get(firstIdx), indices, result);
                if (Objects.isNull(input)) {
                    continue;
                }
                mapSaleItems(rows, indices, input, result);
                if (input.getItems().isEmpty()) {
                    continue;
                }
                saleService.create(input);
                input.getItems().forEach(i -> result.incrementSuccess());

            } catch (Exception ex) {
                result.addError(firstRowNum, ex.getMessage());
            }
        }

        return result;
    }

    private Map<String, List<Integer>> groupRowsByInvoice(List<String[]> rows) {

        Map<String, List<Integer>> groups = new LinkedHashMap<>();

        for (int i = 0; i < rows.size(); i++) {

            String invNum = optional(rows.get(i), 0);

            String key = Optional.ofNullable(invNum)
                    .filter(org.apache.commons.lang3.StringUtils::isNotBlank)
                    .orElse("__row_" + i);

            groups.computeIfAbsent(key, k -> new ArrayList<>()).add(i);
        }

        return groups;
    }

    private SaleInput mapSaleHeader(String[] header,
                                    List<Integer> indices,
                                    ImportResult result) {

        String customerName = optional(header, 1);

        Long customerId = resolveCustomer(customerName, indices, result);

        if (customerName != null && customerId == null) {
            return null;
        }

        SaleInput input = new SaleInput();

        input.setSaleDate(parseSaleDate(optional(header, 0)));
        input.setCustomerId(customerId);
        input.setPaymentStatus(
                Optional.ofNullable(optional(header, 2))
                        .map(String::toUpperCase)
                        .orElse("PAID")
        );
        input.setDiscount(optionalDecimalOrZero(header, 6));
        input.setNotes(optional(header, 7));
        input.setItems(new ArrayList<>());

        return input;
    }

    private Long resolveCustomer(String customerName,
                                 List<Integer> indices,
                                 ImportResult result) {

        return Optional.ofNullable(customerName)
                .filter(org.apache.commons.lang3.StringUtils::isNotBlank)
                .map(customerRepository::findByNameIgnoreCase)
                .flatMap(opt -> {
                    if (opt.isEmpty()) {
                        indices.forEach(i ->
                                result.addError(i + 2, "Customer not found: " + customerName));
                    }
                    return opt;
                })
                .map(CustomerEntity::getId)
                .orElse(null);
    }

    private void mapSaleItems(List<String[]> rows,
                              List<Integer> indices,
                              SaleInput input,
                              ImportResult result) {

        for (Integer idx : indices) {

            int rowNum = idx + 2;

            try {

                String[] r = rows.get(idx);

                String productCode = require(r, 3, "product_code");

                var productOpt = productRepository.findByBarcodeAndDeletedFalse(productCode);

                if (productOpt.isEmpty()) {
                    result.addError(rowNum, "Product not found by code: " + productCode);
                    continue;
                }

                SaleItemInput item = new SaleItemInput();

                item.setProductId(productOpt.get().getId());
                item.setQuantity(requireInt(r, 4, "quantity"));
                item.setUnitPrice(optionalDecimal(r, 5));

                input.getItems().add(item);

            } catch (Exception ex) {
                result.addError(rowNum, ex.getMessage());
            }
        }
    }
}
