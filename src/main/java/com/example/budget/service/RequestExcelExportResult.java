package com.example.budget.service;

/**
 * Результат экспорта заявки в Excel.
 * @param fileName имя файла для скачивания
 * @param content содержимое файла
 */
public record RequestExcelExportResult(String fileName, byte[] content) {
}
