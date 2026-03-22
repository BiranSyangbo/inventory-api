package com.liquorshop.inventory.service.bulk;

import lombok.NoArgsConstructor;
import org.apache.commons.lang3.StringUtils;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Optional;

@NoArgsConstructor
public class BulkImportUtils {

    private static final DateTimeFormatter[] DATE_FORMATS = {
            DateTimeFormatter.ofPattern("yyyy-MM-dd"),
            DateTimeFormatter.ofPattern("dd/MM/yyyy"),
            DateTimeFormatter.ofPattern("MM/dd/yyyy"),
            DateTimeFormatter.ofPattern("dd-MM-yyyy")
    };

    private static final DateTimeFormatter[] DATETIME_FORMATS = {
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"),
            DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss"),
            DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss")
    };

    /* -------------------------
       Base helpers
       ------------------------- */


    private static String get(String[] row, int col) {
        return Optional.ofNullable(row)
                .filter(r -> col < r.length)
                .map(r -> StringUtils.trimToNull(r[col]))
                .orElse(null);
    }

    public static String require(String[] row, int col, String fieldName) {
        return Optional.ofNullable(get(row, col))
                .orElseThrow(() -> new IllegalArgumentException(
                        "Missing required field '" + fieldName + "' (column " + (col + 1) + ")"
                ));
    }


    public static String optional(String[] row, int col) {
        return get(row, col);
    }

    /* -------------------------
       Integer helpers
       ------------------------- */


    public static Integer optionalInt(String[] row, int col) {
        return Optional.ofNullable(get(row, col))
                .map(val -> {
                    try {
                        return Integer.valueOf(val);
                    } catch (NumberFormatException e) {
                        return null;
                    }
                })
                .orElse(null);
    }

    public static int requireInt(String[] row, int col, String fieldName) {
        return Optional.ofNullable(get(row, col))
                .map(val -> {
                    try {
                        return Integer.parseInt(val);
                    } catch (NumberFormatException e) {
                        throw new IllegalArgumentException(
                                "Invalid integer for '" + fieldName + "': " + val
                        );
                    }
                })
                .orElseThrow(() -> new IllegalArgumentException(
                        "Missing required field '" + fieldName + "' (column " + (col + 1) + ")"
                ));
    }

    /* -------------------------
       Decimal helpers
       ------------------------- */

    public static BigDecimal requireDecimal(String[] row, int col, String fieldName) {
        String val = require(row, col, fieldName);

        try {
            val = val.replace(",", "");
            return new BigDecimal(val);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException(
                    "Invalid number for '" + fieldName + "': " + val
            );
        }
    }


    public static BigDecimal optionalDecimal(String[] row, int col) {
        return Optional.ofNullable(get(row, col))
                .map(val -> {
                    try {
                        return new BigDecimal(val);
                    } catch (NumberFormatException e) {
                        return null;
                    }
                })
                .orElse(null);
    }

    public static BigDecimal optionalDecimalOrZero(String[] row, int col) {
        return Optional.ofNullable(optionalDecimal(row, col))
                .orElse(BigDecimal.ZERO);
    }

    /* -------------------------
       Date helpers
       ------------------------- */


    public static LocalDate parseDate(String val) {

        if (StringUtils.isBlank(val)) {
            return null;
        }

        String value = val.trim();

        for (DateTimeFormatter formatter : DATE_FORMATS) {
            try {
                return LocalDate.parse(value, formatter);
            } catch (DateTimeParseException ignored) {
            }
        }

        return null;
    }

    public static LocalDateTime parseSaleDate(String val) {

        if (StringUtils.isBlank(val)) {
            return LocalDateTime.now();
        }

        String value = val.trim();

        return Optional.ofNullable(parseDate(value))
                .map(LocalDate::atStartOfDay)
                .orElseGet(() -> {
                    for (DateTimeFormatter formatter : DATETIME_FORMATS) {
                        try {
                            return LocalDateTime.parse(value, formatter);
                        } catch (DateTimeParseException ignored) {
                        }
                    }
                    return LocalDateTime.now();
                });
    }
}