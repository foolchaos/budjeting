package com.example.budget.service;

import com.example.budget.domain.Cfo;
import com.example.budget.domain.Mvz;
import com.example.budget.domain.RequestPosition;
import com.example.budget.repo.CfoRepository;
import com.example.budget.repo.MvzRepository;
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

import static org.apache.poi.xssf.eventusermodel.XSSFSheetXMLHandler.SheetContentsHandler;

@Service
public class MvzService {
    private static final long MAX_IMPORT_FILE_SIZE = 10 * 1024 * 1024L;
    private static final int MAX_IMPORT_ROWS = 50_000;
    private static final int IMPORT_BATCH_SIZE = 200;

    private final MvzRepository mvzRepository;
    private final RequestPositionRepository requestRepository;
    private final CfoRepository cfoRepository;

    public MvzService(MvzRepository mvzRepository,
                      RequestPositionRepository requestRepository,
                      CfoRepository cfoRepository) {
        this.mvzRepository = mvzRepository;
        this.requestRepository = requestRepository;
        this.cfoRepository = cfoRepository;
    }

    @Transactional(readOnly = true)
    public List<Mvz> findAll() {
        return mvzRepository.findAll();
    }

    public Mvz save(Mvz mvz) {
        return mvzRepository.save(mvz);
    }

    @Transactional
    public void delete(Mvz mvz) {
        if (mvz == null || mvz.getId() == null) {
            return;
        }
        deleteById(mvz.getId());
    }

    @Transactional
    public void deleteById(Long id) {
        if (id == null) {
            return;
        }

        Mvz managed = mvzRepository.findById(id).orElse(null);
        if (managed == null) {
            return;
        }

        List<RequestPosition> requests = requestRepository.findByMvzId(id);
        if (!requests.isEmpty()) {
            requests.forEach(r -> r.setMvz(null));
            requestRepository.saveAll(requests);
        }

        mvzRepository.delete(managed);
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
            Path temp = Files.createTempFile("mvz-import", ".xlsx");
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

        Map<String, Mvz> existingByCode = new HashMap<>();
        for (Mvz mvz : mvzRepository.findAll()) {
            if (mvz.getCode() != null) {
                existingByCode.put(normalizeCodeKey(mvz.getCode()), mvz);
            }
        }

        Map<String, Cfo> cfoByCode = new HashMap<>();
        for (Cfo cfo : cfoRepository.findAll()) {
            if (cfo.getCode() != null) {
                cfoByCode.put(normalizeCodeKey(cfo.getCode()), cfo);
            }
        }

        List<Mvz> batch = new ArrayList<>();
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

                ensureSize(currentRow, 3);
                String rawCode = currentRow.get(0);
                String rawName = currentRow.get(1);
                String rawCfo = currentRow.get(2);

                boolean allBlank = isBlank(rawCode) && isBlank(rawName) && isBlank(rawCfo);
                if (allBlank) {
                    return;
                }
                if (isBlank(rawCode)) {
                    throw new ExcelImportException("Строка " + (rowNum + 1) + ": не указан код МВЗ");
                }
                if (isBlank(rawName)) {
                    throw new ExcelImportException("Строка " + (rowNum + 1) + ": не указано наименование МВЗ");
                }

                String code = rawCode.trim();
                String name = rawName.trim();
                String key = normalizeCodeKey(code);
                Mvz existing = existingByCode.get(key);
                Mvz target = existing != null ? existing : new Mvz();
                target.setCode(code);
                target.setName(name);
                existingByCode.put(key, target);

                String cfoCode = rawCfo != null ? rawCfo.trim() : "";
                if (!cfoCode.isBlank()) {
                    String cfoKey = normalizeCodeKey(cfoCode);
                    Cfo cfo = cfoByCode.get(cfoKey);
                    if (cfo == null) {
                        throw new ExcelImportException("Строка " + (rowNum + 1) + ": ЦФО I с кодом '" + cfoCode + "' не найден");
                    }
                    target.setCfo(cfo);
                } else {
                    target.setCfo(null);
                }

                boolean firstOccurrence = countedCodes.add(key);
                if (firstOccurrence) {
                    if (existing == null) {
                        created.incrementAndGet();
                    } else {
                        updated.incrementAndGet();
                    }
                }
                processed.incrementAndGet();

                if (key != null && batchCodes.add(key)) {
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
            if (e.getCause() instanceof ExcelImportException ie) {
                throw ie;
            }
            throw e;
        } catch (javax.xml.parsers.ParserConfigurationException e) {
            throw new ExcelImportException("Не удалось инициализировать парсер XLSX", e);
        }

        flushBatch(batch, batchCodes, existingByCode);
        return new ExcelImportResult(created.get(), updated.get(), processed.get());
    }

    private void flushBatch(List<Mvz> batch, Set<String> batchCodes, Map<String, Mvz> cache) {
        if (batch.isEmpty()) {
            return;
        }
        List<Mvz> saved = mvzRepository.saveAll(batch);
        for (Mvz savedEntity : saved) {
            if (savedEntity.getCode() != null) {
                cache.put(normalizeCodeKey(savedEntity.getCode()), savedEntity);
            }
        }
        batch.clear();
        batchCodes.clear();
    }

    private void validateHeader(List<String> header) {
        ensureSize(header, 3);
        String codeHeader = header.get(0) != null ? header.get(0).trim() : "";
        String nameHeader = header.get(1) != null ? header.get(1).trim() : "";
        String cfoHeader = header.get(2) != null ? header.get(2).trim() : "";
        if (!"Код".equalsIgnoreCase(codeHeader)
                || !"Наименование".equalsIgnoreCase(nameHeader)
                || !"Код ЦФО I".equalsIgnoreCase(cfoHeader)) {
            throw new ExcelImportException("Некорректный заголовок файла. Ожидаются столбцы 'Код', 'Наименование', 'Код ЦФО I'.");
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

    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
