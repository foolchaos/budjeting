package com.example.budget.service;

/**
 * Исключение, выбрасываемое при ошибках импорта БДЗ из Excel.
 */
public class BdzImportException extends RuntimeException {
    public BdzImportException(String message) {
        super(message);
    }

    public BdzImportException(String message, Throwable cause) {
        super(message, cause);
    }
}
