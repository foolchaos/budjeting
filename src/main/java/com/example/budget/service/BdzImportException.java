package com.example.budget.service;

public class BdzImportException extends RuntimeException {

    public BdzImportException(String message) {
        super(message);
    }

    public BdzImportException(String message, Throwable cause) {
        super(message, cause);
    }
}
