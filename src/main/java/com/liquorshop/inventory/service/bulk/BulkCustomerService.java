package com.liquorshop.inventory.service.bulk;

import com.liquorshop.inventory.dto.CustomerRequest;
import com.liquorshop.inventory.dto.ImportResult;
import com.liquorshop.inventory.service.CustomerService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;
import java.util.List;

import static com.liquorshop.inventory.service.bulk.BulkImportUtils.*;
import static com.liquorshop.inventory.service.bulk.FileHandler.*;

@Service
@RequiredArgsConstructor
public class BulkCustomerService {

    private final CustomerService customerService;


    public ImportResult importCustomers(MultipartFile file) throws Exception {
        List<String[]> rows = parseFile(file);
        ImportResult result = new ImportResult();
        result.setTotalRows(rows.size());

        for (int i = 0; i < rows.size(); i++) {
            int rowNum = i + 2;
            try {
                String[] r = rows.get(i);
                CustomerRequest req = new CustomerRequest();
                req.setName(require(r, 0, "name"));
                req.setPhone(optional(r, 1));
                req.setAddress(optional(r, 2));
                String creditLimitStr = optional(r, 3);
                req.setCreditLimit(creditLimitStr != null && !creditLimitStr.isBlank()
                        ? new BigDecimal(creditLimitStr.trim())
                        : BigDecimal.ZERO);

                customerService.create(req);
                result.incrementSuccess();
            } catch (Exception ex) {
                result.addError(rowNum, ex.getMessage());
            }
        }
        return result;
    }

}
