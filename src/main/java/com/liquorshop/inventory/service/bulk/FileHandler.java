package com.liquorshop.inventory.service.bulk;

import lombok.NoArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.apache.poi.ss.usermodel.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@NoArgsConstructor
public class FileHandler {

    private static final DataFormatter FORMATTER = new DataFormatter();

    public static List<String[]> parseFile(MultipartFile file) throws Exception {

        String filename = Optional.ofNullable(file.getOriginalFilename())
                .map(String::toLowerCase)
                .orElse("");

        if (filename.endsWith(".csv")) {
            return parseCsv(file.getInputStream());
        }

        if (filename.endsWith(".xlsx") || filename.endsWith(".xls")) {
            return parseExcel(file.getInputStream());
        }

        String contentType = Optional.ofNullable(file.getContentType())
                .orElse("");

        return (contentType.contains("spreadsheet") || contentType.contains("excel"))
                ? parseExcel(file.getInputStream())
                : parseCsv(file.getInputStream());
    }

    private static List<String[]> parseCsv(InputStream is) throws Exception {

        try (var reader = new com.opencsv.CSVReader(
                new java.io.InputStreamReader(is, StandardCharsets.UTF_8))) {

            List<String[]> all = reader.readAll();

            if (all.size() <= 1) {
                return List.of();
            }

            return all.subList(1, all.size()); // skip header
        }
    }

    private static List<String[]> parseExcel(InputStream is) throws Exception {

        List<String[]> rows = new ArrayList<>();

        try (Workbook wb = WorkbookFactory.create(is)) {

            Sheet sheet = wb.getSheetAt(0);

            int lastRow = sheet.getLastRowNum();
            if (lastRow < 1) {
                return rows;
            }

            Row firstDataRow = sheet.getRow(1);

            int maxCols = Optional.ofNullable(firstDataRow)
                    .map(Row::getLastCellNum)
                    .orElse((short) 20);

            for (int i = 1; i <= lastRow; i++) {

                Row row = sheet.getRow(i);
                if (row == null) {
                    continue;
                }

                String[] cells = new String[maxCols];

                for (int c = 0; c < maxCols; c++) {
                    cells[c] = cellString(row.getCell(c));
                }

                rows.add(cells);
            }
        }

        return rows;
    }

    private static String cellString(Cell cell) {

        return Optional.ofNullable(cell)
                .map(FORMATTER::formatCellValue)
                .map(StringUtils::trimToEmpty)
                .orElse("");
    }
}