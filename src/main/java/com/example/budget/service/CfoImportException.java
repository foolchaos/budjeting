package com.example.budget.service;

/**
 * Исключение, сигнализирующее об ошибке при импорте ЦФО I из Excel.
 */
public class CfoImportException extends RuntimeException {
    public CfoImportException(String message) {
        super(message);
    }

    public CfoImportException(String message, Throwable cause) {
        super(message, cause);
    }
}
