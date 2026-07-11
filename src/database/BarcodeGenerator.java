package database;

import java.util.*;

public class BarcodeGenerator {
    
    public static String generateBarcode(String itemCode) {
        return generateEAN13(itemCode);
    }
    
    public static String generateEAN13(String itemCode) {
        long code = Long.parseLong(itemCode);
        String barcode = String.format("%012d", code);
        
        int checkDigit = calculateEAN13CheckDigit(barcode);
        return barcode + checkDigit;
    }
    
    public static int calculateEAN13CheckDigit(String code) {
        int sum = 0;
        for (int i = 0; i < code.length(); i++) {
            int digit = Character.getNumericValue(code.charAt(i));
            if (i % 2 == 0) {
                sum += digit;
            } else {
                sum += digit * 3;
            }
        }
        return (10 - (sum % 10)) % 10;
    }
    
    public static boolean validateBarcode(String barcode) {
        if (barcode.length() != 13) {
            return false;
        }
        try {
            String code = barcode.substring(0, 12);
            int checkDigit = Integer.parseInt(barcode.substring(12));
            return calculateEAN13CheckDigit(code) == checkDigit;
        } catch (NumberFormatException e) {
            return false;
        }
    }
    
    public static String generateInvoiceNumber(String date) {
        String invoicePrefix = "INV-" + date.replace("-", "");
        return invoicePrefix + "-" + System.nanoTime() % 10000;
    }
}
