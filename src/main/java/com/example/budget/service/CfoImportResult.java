package com.example.budget.service;

/**
 * Результат импорта данных ЦФО из Excel.
 * @param created количество созданных записей
 * @param updated количество обновлённых записей
 * @param processed всего обработанных строк данных
 */
public record CfoImportResult(int created, int updated, int processed) {
}
