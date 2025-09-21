package com.dsai.rag.api.util;

import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import java.io.FileInputStream;
import java.io.IOException;

public class ExcelHeaderChecker {
    public static void main(String[] args) {
        String filePath = "/Users/ijeongseob/IdeaProjects/ds-ai2/item/doubless/doubless.xlsx";
        
        try (FileInputStream fis = new FileInputStream(filePath);
             Workbook workbook = new XSSFWorkbook(fis)) {
            
            Sheet sheet = workbook.getSheetAt(0);
            Row headerRow = sheet.getRow(0);
            
            System.out.println("Headers in doubless.xlsx:");
            if (headerRow != null) {
                for (Cell cell : headerRow) {
                    if (cell != null) {
                        String value = getCellValueAsString(cell);
                        System.out.println("Column " + cell.getColumnIndex() + ": " + value);
                    }
                }
            }
            
            // Print first data row to understand structure
            System.out.println("\nFirst data row:");
            Row dataRow = sheet.getRow(1);
            if (dataRow != null) {
                for (Cell cell : dataRow) {
                    if (cell != null) {
                        String value = getCellValueAsString(cell);
                        System.out.println("Column " + cell.getColumnIndex() + ": " + value);
                    }
                }
            }
            
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    
    private static String getCellValueAsString(Cell cell) {
        if (cell == null) return "";
        
        switch (cell.getCellType()) {
            case STRING:
                return cell.getStringCellValue();
            case NUMERIC:
                if (DateUtil.isCellDateFormatted(cell)) {
                    return cell.getDateCellValue().toString();
                }
                return String.valueOf((long) cell.getNumericCellValue());
            case BOOLEAN:
                return String.valueOf(cell.getBooleanCellValue());
            case FORMULA:
                return cell.getCellFormula();
            default:
                return "";
        }
    }
}