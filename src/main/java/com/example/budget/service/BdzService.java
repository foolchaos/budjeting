package com.example.budget.service;

import com.example.budget.domain.Bdz;
import com.example.budget.domain.Request;
import com.example.budget.repo.BdzRepository;
import com.example.budget.repo.RequestRepository;
import com.monitorjbl.xlsx.StreamingReader;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.hibernate.Hibernate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

@Service
public class BdzService {
    private static final int MAX_IMPORT_ROWS = 10000;

    private final BdzRepository bdzRepository;
    private final RequestRepository requestRepository;

    public BdzService(BdzRepository bdzRepository, RequestRepository requestRepository) {
        this.bdzRepository = bdzRepository;
        this.requestRepository = requestRepository;
    }

    @Transactional(readOnly = true)
    public List<Bdz> findAll() {
        List<Bdz> list = bdzRepository.findAll();
        list.forEach(b -> {
            Hibernate.initialize(b);
            if (b.getParent() != null) {
                Hibernate.initialize(b.getParent());
            }
        });
        return list;
    }

    @Transactional(readOnly = true)
    public java.util.List<Bdz> findRoots() {
        List<Bdz> list = bdzRepository.findByParentIsNull();
        list.forEach(Hibernate::initialize);
        return list;
    }

    @Transactional(readOnly = true)
    public java.util.List<Bdz> findChildren(Long parentId) {
        List<Bdz> list = bdzRepository.findByParentId(parentId);
        list.forEach(b -> {
            Hibernate.initialize(b);
            if (b.getParent() != null) {
                Hibernate.initialize(b.getParent());
            }
        });
        return list;
    }

    @Transactional(readOnly = true)
    public Bdz findById(Long id) {
        Bdz bdz = bdzRepository.findById(id).orElse(null);
        if (bdz != null) {
            Hibernate.initialize(bdz);
            if (bdz.getParent() != null) {
                Hibernate.initialize(bdz.getParent());
            }
        }
        return bdz;
    }

    public Bdz save(Bdz bdz) { return bdzRepository.save(bdz); }

    @Transactional
    public void deleteById(Long id) {
        Bdz bdz = bdzRepository.findById(id).orElse(null);
        if (bdz == null) return;
        // Отвязать заявки (bdz -> null)
        List<Request> requests = requestRepository.findAll();
        for (Request r : requests) {
            if (r.getBdz() != null && r.getBdz().getId().equals(id)) {
                r.setBdz(null);
                r.setZgd(null);
                requestRepository.save(r);
            }
        }
        // Каскад: потомки/БО/ЗГД настроены на уровне сущностей
        bdzRepository.delete(bdz);
    }

    @Transactional
    public BdzImportResult importFromExcel(InputStream inputStream) {
        DataFormatter formatter = new DataFormatter();
        Map<String, Bdz> existingByCode = new LinkedHashMap<>();
        for (Bdz item : bdzRepository.findAll()) {
            if (item.getCode() != null) {
                existingByCode.put(item.getCode(), item);
            }
        }

        Set<String> seenCodes = new LinkedHashSet<>();
        Map<String, String> parentByCode = new LinkedHashMap<>();
        Set<String> touchedCodes = new LinkedHashSet<>();
        int created = 0;
        int updated = 0;
        int dataRows = 0;

        try (StreamingReader workbook = StreamingReader.builder()
                .rowCacheSize(100)
                .bufferSize(4096)
                .open(inputStream)) {
            Iterator<Sheet> sheetIterator = workbook.iterator();
            if (!sheetIterator.hasNext()) {
                throw new BdzImportException("Файл не содержит листов");
            }

            Sheet sheet = sheetIterator.next();
            Iterator<Row> rowIterator = sheet.iterator();
            if (!rowIterator.hasNext()) {
                throw new BdzImportException("Файл не содержит данных");
            }

            Row header = rowIterator.next();
            validateHeader(header, formatter);

            while (rowIterator.hasNext()) {
                Row row = rowIterator.next();
                int excelRowNumber = row.getRowNum() + 1;

                String code = normalizeCellValue(getCellValue(row, 0, formatter));
                String name = normalizeCellValue(getCellValue(row, 1, formatter));
                String parentCode = normalizeCellValue(getCellValue(row, 2, formatter));

                if (code == null && name == null && parentCode == null) {
                    continue;
                }

                if (code == null) {
                    throw new BdzImportException("Строка " + excelRowNumber + ": не заполнен код");
                }
                if (name == null) {
                    throw new BdzImportException("Строка " + excelRowNumber + ": не заполнено наименование");
                }

                if (!seenCodes.add(code)) {
                    throw new BdzImportException("Строка " + excelRowNumber + ": код '" + code + "' повторяется в файле");
                }

                dataRows++;
                if (dataRows > MAX_IMPORT_ROWS) {
                    throw new BdzImportException("Файл содержит более " + MAX_IMPORT_ROWS + " строк данных");
                }

                Bdz entity = existingByCode.get(code);
                boolean isNew = false;
                if (entity == null) {
                    entity = new Bdz();
                    entity.setCode(code);
                    existingByCode.put(code, entity);
                    isNew = true;
                }
                entity.setName(name);
                entity.setParent(null);

                parentByCode.put(code, parentCode);
                touchedCodes.add(code);

                if (isNew) {
                    created++;
                } else {
                    updated++;
                }
            }
        } catch (BdzImportException e) {
            throw e;
        } catch (Exception e) {
            throw new BdzImportException("Не удалось прочитать файл Excel", e);
        }

        if (dataRows == 0) {
            throw new BdzImportException("Файл не содержит строк с данными");
        }

        List<Bdz> firstPass = touchedCodes.stream()
                .map(existingByCode::get)
                .toList();
        bdzRepository.saveAll(firstPass);

        List<Bdz> secondPass = new ArrayList<>();
        for (String code : touchedCodes) {
            Bdz child = existingByCode.get(code);
            String parentCode = parentByCode.get(code);
            if (parentCode == null) {
                child.setParent(null);
            } else {
                if (Objects.equals(code, parentCode)) {
                    throw new BdzImportException("Элемент с кодом '" + code + "' не может быть родителем сам себе");
                }
                Bdz parent = existingByCode.get(parentCode);
                if (parent == null) {
                    throw new BdzImportException("Для элемента с кодом '" + code + "' не найден родитель '" + parentCode + "'");
                }
                child.setParent(parent);
            }
            secondPass.add(child);
        }
        bdzRepository.saveAll(secondPass);

        return new BdzImportResult(dataRows, created, updated);
    }

    private void validateHeader(Row header, DataFormatter formatter) {
        String code = normalizeHeaderValue(header, 0, formatter);
        String name = normalizeHeaderValue(header, 1, formatter);
        String parent = normalizeHeaderValue(header, 2, formatter);
        if (!"код".equals(code) || !"наименование".equals(name) || !"родительский код".equals(parent)) {
            throw new BdzImportException("Первая строка должна содержать заголовки: Код, Наименование, Родительский код");
        }
    }

    private String normalizeHeaderValue(Row row, int cellIndex, DataFormatter formatter) {
        String value = normalizeCellValue(getCellValue(row, cellIndex, formatter));
        return value != null ? value.toLowerCase(Locale.ROOT) : null;
    }

    private String getCellValue(Row row, int cellIndex, DataFormatter formatter) {
        if (row == null) {
            return null;
        }
        Cell cell = row.getCell(cellIndex, Row.MissingCellPolicy.RETURN_BLANK_AS_NULL);
        if (cell == null) {
            return null;
        }
        return formatter.formatCellValue(cell);
    }

    private String normalizeCellValue(String raw) {
        if (raw == null) {
            return null;
        }
        String trimmed = raw.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
