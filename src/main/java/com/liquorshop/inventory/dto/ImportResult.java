package com.liquorshop.inventory.dto;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class ImportResult {

    private int totalRows;
    private int successCount;
    private int failureCount;
    private List<RowError> errors = new ArrayList<>();

    public void addError(int rowNumber, String message) {
        errors.add(new RowError(rowNumber, message));
        failureCount++;
    }

    public void incrementSuccess() {
        successCount++;
    }

    @Data
    public static class RowError {
        private final int row;
        private final String message;
    }
}
