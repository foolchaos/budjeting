package com.example.budget.service;

import com.example.budget.domain.ProcurementMethod;
import com.example.budget.domain.RequestPosition;
import com.example.budget.repo.ProcurementMethodRepository;
import com.example.budget.repo.RequestPositionRepository;
import org.apache.poi.ooxml.util.SAXHelper;
import org.apache.poi.openxml4j.exceptions.OpenXML4JException;
import org.apache.poi.openxml4j.opc.OPCPackage;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.xssf.eventusermodel.XSSFReader;
import org.apache.poi.xssf.eventusermodel.XSSFSheetXMLHandler;
import org.apache.poi.xssf.model.SharedStrings;
import org.apache.poi.xssf.model.StylesTable;
import org.apache.poi.xssf.usermodel.XSSFComment;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

@Service
public class ProcurementMethodService {

    private static final long MAX_IMPORT_FILE_SIZE = 10 * 1024 * 1024L;
    private static final int MAX_IMPORT_ROWS = 50_000;
    private static final int IMPORT_BATCH_SIZE = 200;

    private final ProcurementMethodRepository procurementMethodRepository;
    private final RequestPositionRepository requestRepository;

    public ProcurementMethodService(ProcurementMethodRepository procurementMethodRepository,
                                    RequestPositionRepository requestRepository) {
        this.procurementMethodRepository = procurementMethodRepository;
        this.requestRepository = requestRepository;
    }

    @Transactional(readOnly = true)
    public List<ProcurementMethod> findAll() {
        return procurementMethodRepository.findAll();
    }

    public ProcurementMethod save(ProcurementMethod method) {
        return procurementMethodRepository.save(method);
    }

    @Transactional
    public void delete(ProcurementMethod method) {
        if (method == null || method.getId() == null) {
            return;
        }
        deleteById(method.getId());
    }

    @Transactional
    public void deleteById(Long id) {
        if (id == null) {
            return;
        }
        ProcurementMethod managed = procurementMethodRepository.findById(id).orElse(null);
        if (managed == null) {
            return;
        }
        List<RequestPosition> requests = requestRepository.findByProcurementMethodId(id);
        if (!requests.isEmpty()) {
            requests.forEach(r -> r.setProcurementMethod(null));
            requestRepository.saveAll(requests);
        }
        procurementMethodRepository.delete(managed);
    }

    @Transactional
    public ExcelImportResult importFromXlsx(Path file) {
        try {
            if (Files.notExists(file)) {
                throw new ExcelImportException("Файл импорта не найден");
            }
            long size = Files.size(file);
            if (size > MAX_IMPORT_FILE_SIZE) {
                throw new ExcelImportException("Размер файла превышает 10 МБ");
            }
            try (OPCPackage pkg = OPCPackage.open(file.toFile())) {
                return readWorkbook(pkg);
            }
        } catch (ExcelImportException e) {
            throw e;
        } catch (IOException | OpenXML4JException | SAXException e) {
            throw new ExcelImportException("Не удалось обработать файл импорта", e);
        }
    }

    @Transactional
    public ExcelImportResult importFromXlsx(InputStream inputStream) {
        try {
            Path temp = Files.createTempFile("procurement-method-import", ".xlsx");
            try (var out = Files.newOutputStream(temp)) {
                inputStream.transferTo(out);
            }
            try {
                return importFromXlsx(temp);
            } finally {
                Files.deleteIfExists(temp);
            }
        } catch (IOException e) {
            throw new ExcelImportException("Не удалось прочитать файл импорта", e);
        }
    }

    private ExcelImportResult readWorkbook(OPCPackage pkg)
            throws IOException, OpenXML4JException, SAXException {
        XSSFReader reader = new XSSFReader(pkg);
        StylesTable styles = reader.getStylesTable();
        SharedStrings sharedStrings = reader.getSharedStringsTable();
        XSSFReader.SheetIterator sheets = (XSSFReader.SheetIterator) reader.getSheetsData();
        if (!sheets.hasNext()) {
            throw new ExcelImportException("В файле нет ни одного листа");
        }

        Map<String, ProcurementMethod> existingByName = new HashMap<>();
        for (ProcurementMethod method : procurementMethodRepository.findAll()) {
            if (method.getName() != null) {
                existingByName.put(normalizeNameKey(method.getName()), method);
            }
        }

        List<ProcurementMethod> batch = new ArrayList<>();
        Set<String> batchNames = new HashSet<>();
        Set<String> countedNames = new HashSet<>();
        AtomicInteger created = new AtomicInteger();
        AtomicInteger updated = new AtomicInteger();
        AtomicInteger processed = new AtomicInteger();

        XSSFSheetXMLHandler.SheetContentsHandler handler = new XSSFSheetXMLHandler.SheetContentsHandler() {
            private final List<String> currentRow = new ArrayList<>();
            private int lastColumn = -1;

            @Override
            public void startRow(int rowNum) {
                currentRow.clear();
                lastColumn = -1;
            }

            @Override
            public void endRow(int rowNum) {
                if (rowNum == 0) {
                    validateHeader(currentRow);
                    return;
                }
                if (rowNum > MAX_IMPORT_ROWS) {
                    throw new ExcelImportException("Файл содержит больше 50 000 строк");
                }

                ensureSize(currentRow, 1);
                String rawName = currentRow.get(0);
                if (rawName == null || rawName.isBlank()) {
                    return;
                }

                String name = rawName.trim();
                String key = normalizeNameKey(name);
                ProcurementMethod existing = existingByName.get(key);
                ProcurementMethod target = existing != null ? existing : new ProcurementMethod();
                target.setName(name);
                existingByName.put(key, target);

                boolean firstOccurrence = countedNames.add(key);
                if (firstOccurrence) {
                    if (existing == null) {
                        created.incrementAndGet();
                    } else {
                        updated.incrementAndGet();
                    }
                }
                processed.incrementAndGet();

                if (batchNames.add(key)) {
                    batch.add(target);
                }
                if (batch.size() >= IMPORT_BATCH_SIZE) {
                    flushBatch(batch, batchNames, existingByName);
                }
            }

            @Override
            public void cell(String cellReference, String formattedValue, XSSFComment comment) {
                int columnIndex = cellReference != null ? columnIndex(cellReference) : lastColumn + 1;
                lastColumn = columnIndex;
                ensureSize(currentRow, columnIndex + 1);
                currentRow.set(columnIndex, formattedValue != null ? formattedValue.trim() : "");
            }

            @Override
            public void headerFooter(String text, boolean isHeader, String tagName) {
                // not used
            }
        };

        try (InputStream sheetStream = sheets.next()) {
            XMLReader parser = SAXHelper.newXMLReader();
            DataFormatter formatter = new DataFormatter(Locale.getDefault());
            parser.setContentHandler(new XSSFSheetXMLHandler(styles, sharedStrings, handler, formatter, false));
            parser.parse(new InputSource(sheetStream));
        } catch (ExcelImportException e) {
            throw e;
        } catch (SAXException e) {
            if (e.getCause() instanceof ExcelImportException cie) {
                throw cie;
            }
            throw e;
        } catch (javax.xml.parsers.ParserConfigurationException e) {
            throw new ExcelImportException("Не удалось инициализировать парсер XLSX", e);
        }

        flushBatch(batch, batchNames, existingByName);
        return new ExcelImportResult(created.get(), updated.get(), processed.get());
    }

    private void flushBatch(List<ProcurementMethod> batch,
                            Set<String> batchNames,
                            Map<String, ProcurementMethod> cache) {
        if (batch.isEmpty()) {
            return;
        }
        List<ProcurementMethod> saved = procurementMethodRepository.saveAll(batch);
        for (ProcurementMethod method : saved) {
            if (method.getName() != null) {
                cache.put(normalizeNameKey(method.getName()), method);
            }
        }
        batch.clear();
        batchNames.clear();
    }

    private void validateHeader(List<String> header) {
        ensureSize(header, 1);
        String value = header.get(0) != null ? header.get(0).trim() : "";
        if (!("Наименование".equalsIgnoreCase(value) || "Способ закупки".equalsIgnoreCase(value))) {
            throw new ExcelImportException(
                    "Некорректный заголовок файла. Ожидается столбец 'Наименование'.");
        }
    }

    private static void ensureSize(List<String> row, int size) {
        while (row.size() < size) {
            row.add("");
        }
    }

    private static int columnIndex(String cellReference) {
        int index = 0;
        for (int i = 0; i < cellReference.length(); i++) {
            char c = cellReference.charAt(i);
            if (Character.isLetter(c)) {
                index = index * 26 + (Character.toUpperCase(c) - 'A' + 1);
            } else {
                break;
            }
        }
        return index - 1;
    }

    private static String normalizeNameKey(String name) {
        return name == null ? null : name.trim().toUpperCase(Locale.ROOT);
    }
}
