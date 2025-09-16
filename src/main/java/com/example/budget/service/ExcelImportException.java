package com.example.budget.service;

/**
 * Исключение, сигнализирующее об ошибке при импорте данных из Excel.
 */
public class ExcelImportException extends RuntimeException {
    public ExcelImportException(String message) {
        super(message);
    }

    public ExcelImportException(String message, Throwable cause) {
        super(message, cause);
    }
}
