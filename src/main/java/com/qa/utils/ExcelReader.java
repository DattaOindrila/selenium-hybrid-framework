package com.qa.utils;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Reads a sheet of testdata.xlsx into a shape TestNG's @DataProvider can return.
 *
 * Row 1 is treated as the header. Each remaining row becomes a Map of
 * column-name -> cell value, so a test reads row.get("email") instead of row[2].
 * That matters in practice: inserting a column in the spreadsheet then does not
 * silently shift every index in every test.
 *
 * Every cell is read through DataFormatter, which returns the string exactly as
 * Excel displays it. Without it a zip code typed as 700001 comes back as the
 * double "700001.0" and the assertion fails for a reason that has nothing to do
 * with the application under test.
 */
public final class ExcelReader {

    private static final Logger log = LogManager.getLogger(ExcelReader.class);
    private static final String DEFAULT_WORKBOOK = "testdata/testdata.xlsx";
    private static final DataFormatter FORMATTER = new DataFormatter();

    private ExcelReader() {
    }

    /** Reads a sheet from the default workbook on the test classpath. */
    public static List<Map<String, String>> readSheet(String sheetName) {
        return readSheet(DEFAULT_WORKBOOK, sheetName);
    }

    public static List<Map<String, String>> readSheet(String classpathResource, String sheetName) {
        List<Map<String, String>> rows = new ArrayList<>();

        try (InputStream in = ExcelReader.class.getClassLoader().getResourceAsStream(classpathResource)) {
            if (in == null) {
                throw new IllegalStateException(
                        "Workbook '" + classpathResource + "' not found on the test classpath.");
            }
            try (Workbook workbook = new XSSFWorkbook(in)) {
                Sheet sheet = workbook.getSheet(sheetName);
                if (sheet == null) {
                    throw new IllegalStateException(
                            "Sheet '" + sheetName + "' not found in " + classpathResource
                            + ". Available: " + sheetNames(workbook));
                }

                Row header = sheet.getRow(sheet.getFirstRowNum());
                if (header == null) {
                    throw new IllegalStateException("Sheet '" + sheetName + "' has no header row.");
                }

                List<String> columns = new ArrayList<>();
                for (int c = 0; c < header.getLastCellNum(); c++) {
                    columns.add(FORMATTER.formatCellValue(header.getCell(c)).trim());
                }

                for (int r = sheet.getFirstRowNum() + 1; r <= sheet.getLastRowNum(); r++) {
                    Row row = sheet.getRow(r);
                    if (row == null || isBlank(row, columns.size())) {
                        continue; // tolerate blank spacer rows
                    }
                    Map<String, String> record = new LinkedHashMap<>();
                    for (int c = 0; c < columns.size(); c++) {
                        Cell cell = row.getCell(c);
                        record.put(columns.get(c), FORMATTER.formatCellValue(cell).trim());
                    }
                    rows.add(record);
                }
            }
        } catch (IOException e) {
            throw new IllegalStateException("Failed to read " + classpathResource, e);
        }

        log.info("Read {} data row(s) from sheet '{}'", rows.size(), sheetName);
        if (rows.isEmpty()) {
            throw new IllegalStateException(
                    "Sheet '" + sheetName + "' contained a header but no data rows. "
                    + "A data-driven test with zero rows silently passes, so this fails instead.");
        }
        return rows;
    }

    /** Wraps each row in a single-element Object[], the shape @DataProvider expects. */
    public static Object[][] asDataProvider(String sheetName) {
        List<Map<String, String>> rows = readSheet(sheetName);
        Object[][] data = new Object[rows.size()][1];
        for (int i = 0; i < rows.size(); i++) {
            data[i][0] = rows.get(i);
        }
        return data;
    }

    private static boolean isBlank(Row row, int columnCount) {
        for (int c = 0; c < columnCount; c++) {
            if (!FORMATTER.formatCellValue(row.getCell(c)).trim().isEmpty()) {
                return false;
            }
        }
        return true;
    }

    private static List<String> sheetNames(Workbook workbook) {
        List<String> names = new ArrayList<>();
        for (int i = 0; i < workbook.getNumberOfSheets(); i++) {
            names.add(workbook.getSheetName(i));
        }
        return names;
    }
}
