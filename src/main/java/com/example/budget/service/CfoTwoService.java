package com.example.budget.service;

import com.example.budget.domain.CfoTwo;
import com.example.budget.repo.CfoTwoRepository;
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

import static org.apache.poi.xssf.eventusermodel.XSSFSheetXMLHandler.SheetContentsHandler;

@Service
public class CfoTwoService {
    private static final long MAX_IMPORT_FILE_SIZE = 10 * 1024 * 1024L;
    private static final int MAX_IMPORT_ROWS = 50_000;
    private static final int IMPORT_BATCH_SIZE = 200;

    private final CfoTwoRepository cfoTwoRepository;

    public CfoTwoService(CfoTwoRepository cfoTwoRepository) {
        this.cfoTwoRepository = cfoTwoRepository;
    }

    public List<CfoTwo> findAll() { return cfoTwoRepository.findAll(); }
    public CfoTwo save(CfoTwo cfoTwo) { return cfoTwoRepository.save(cfoTwo); }

    @Transactional
    public void deleteById(Long id) {
        cfoTwoRepository.deleteById(id);
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
            Path temp = Files.createTempFile("cfo-two-import", ".xlsx");
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

    private ExcelImportResult readWorkbook(OPCPackage pkg) throws IOException, OpenXML4JException, SAXException {
        XSSFReader reader = new XSSFReader(pkg);
        StylesTable styles = reader.getStylesTable();
        SharedStrings sharedStrings = reader.getSharedStringsTable();
        XSSFReader.SheetIterator sheets = (XSSFReader.SheetIterator) reader.getSheetsData();
        if (!sheets.hasNext()) {
            throw new ExcelImportException("В файле нет ни одного листа");
        }

        Map<String, CfoTwo> existingByCode = new HashMap<>();
        for (CfoTwo cfo : cfoTwoRepository.findAll()) {
            if (cfo.getCode() != null) {
                existingByCode.put(normalizeCodeKey(cfo.getCode()), cfo);
            }
        }

        List<CfoTwo> batch = new ArrayList<>();
        Set<String> batchCodes = new HashSet<>();
        Set<String> countedCodes = new HashSet<>();
        AtomicInteger created = new AtomicInteger();
        AtomicInteger updated = new AtomicInteger();
        AtomicInteger processed = new AtomicInteger();

        SheetContentsHandler handler = new SheetContentsHandler() {
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

                ensureSize(currentRow, 2);
                String rawCode = currentRow.get(0);
                String rawName = currentRow.get(1);
                if ((rawCode == null || rawCode.isBlank()) && (rawName == null || rawName.isBlank())) {
                    return;
                }
                if (rawCode == null || rawCode.isBlank()) {
                    throw new ExcelImportException("Строка " + (rowNum + 1) + ": не указан код ЦФО II");
                }
                if (rawName == null || rawName.isBlank()) {
                    throw new ExcelImportException("Строка " + (rowNum + 1) + ": не указано наименование ЦФО II");
                }

                String code = rawCode.trim();
                String name = rawName.trim();
                String key = normalizeCodeKey(code);
                CfoTwo existing = existingByCode.get(key);
                CfoTwo target = existing != null ? existing : new CfoTwo();
                target.setCode(code);
                target.setName(name);
                existingByCode.put(key, target);

                boolean firstOccurrence = countedCodes.add(key);
                if (firstOccurrence) {
                    if (existing == null) {
                        created.incrementAndGet();
                    } else {
                        updated.incrementAndGet();
                    }
                }
                processed.incrementAndGet();

                if (batchCodes.add(key)) {
                    batch.add(target);
                }
                if (batch.size() >= IMPORT_BATCH_SIZE) {
                    flushBatch(batch, batchCodes, existingByCode);
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

        flushBatch(batch, batchCodes, existingByCode);
        return new ExcelImportResult(created.get(), updated.get(), processed.get());
    }

    private void flushBatch(List<CfoTwo> batch, Set<String> batchCodes, Map<String, CfoTwo> cache) {
        if (batch.isEmpty()) {
            return;
        }
        List<CfoTwo> saved = cfoTwoRepository.saveAll(batch);
        for (CfoTwo savedEntity : saved) {
            if (savedEntity.getCode() != null) {
                cache.put(normalizeCodeKey(savedEntity.getCode()), savedEntity);
            }
        }
        batch.clear();
        batchCodes.clear();
    }

    private void validateHeader(List<String> header) {
        ensureSize(header, 2);
        String codeHeader = header.get(0) != null ? header.get(0).trim() : "";
        String nameHeader = header.get(1) != null ? header.get(1).trim() : "";
        if (!"Код".equalsIgnoreCase(codeHeader) || !"Наименование".equalsIgnoreCase(nameHeader)) {
            throw new ExcelImportException("Некорректный заголовок файла. Первые два столбца должны быть 'Код' и 'Наименование'.");
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

    private static String normalizeCodeKey(String code) {
        return code == null ? null : code.trim().toUpperCase(Locale.ROOT);
    }
}
