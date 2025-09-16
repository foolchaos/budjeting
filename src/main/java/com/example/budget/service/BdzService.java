package com.example.budget.service;

import com.example.budget.domain.Bdz;
import com.example.budget.domain.Cfo;
import com.example.budget.domain.Request;
import com.example.budget.repo.BdzRepository;
import com.example.budget.repo.CfoRepository;
import com.example.budget.repo.RequestRepository;
import org.apache.poi.ooxml.util.SAXHelper;
import org.apache.poi.openxml4j.exceptions.OpenXML4JException;
import org.apache.poi.openxml4j.opc.OPCPackage;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.xssf.eventusermodel.XSSFReader;
import org.apache.poi.xssf.eventusermodel.XSSFSheetXMLHandler;
import org.apache.poi.xssf.model.SharedStrings;
import org.apache.poi.xssf.model.StylesTable;
import org.apache.poi.xssf.usermodel.XSSFComment;
import org.hibernate.Hibernate;
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
import java.util.List;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

import static org.apache.poi.xssf.eventusermodel.XSSFSheetXMLHandler.SheetContentsHandler;

@Service
public class BdzService {
    private static final long MAX_IMPORT_FILE_SIZE = 10 * 1024 * 1024L;
    private static final int MAX_IMPORT_ROWS = 50_000;
    private static final int IMPORT_BATCH_SIZE = 200;

    private final BdzRepository bdzRepository;
    private final RequestRepository requestRepository;
    private final CfoRepository cfoRepository;

    public BdzService(BdzRepository bdzRepository, RequestRepository requestRepository, CfoRepository cfoRepository) {
        this.bdzRepository = bdzRepository;
        this.requestRepository = requestRepository;
        this.cfoRepository = cfoRepository;
    }

    @Transactional(readOnly = true)
    public List<Bdz> findAll() {
        List<Bdz> list = bdzRepository.findAll();
        list.forEach(b -> {
            Hibernate.initialize(b);
            if (b.getParent() != null) {
                Hibernate.initialize(b.getParent());
            }
            if (b.getCfo() != null) {
                Hibernate.initialize(b.getCfo());
            }
        });
        return list;
    }

    @Transactional(readOnly = true)
    public java.util.List<Bdz> findRoots() {
        List<Bdz> list = bdzRepository.findByParentIsNull();
        list.forEach(b -> {
            Hibernate.initialize(b);
            if (b.getCfo() != null) {
                Hibernate.initialize(b.getCfo());
            }
        });
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
            if (b.getCfo() != null) {
                Hibernate.initialize(b.getCfo());
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
            if (bdz.getCfo() != null) {
                Hibernate.initialize(bdz.getCfo());
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
            Path temp = Files.createTempFile("bdz-import", ".xlsx");
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

        Map<String, Bdz> existingByCode = new HashMap<>();
        for (Bdz bdz : bdzRepository.findAll()) {
            if (bdz.getCode() != null) {
                existingByCode.put(normalizeCodeKey(bdz.getCode()), bdz);
            }
        }

        Map<String, Cfo> cfoByCode = new HashMap<>();
        for (Cfo cfo : cfoRepository.findAll()) {
            if (cfo.getCode() != null) {
                cfoByCode.put(normalizeCodeKey(cfo.getCode()), cfo);
            }
        }

        List<Bdz> batch = new ArrayList<>();
        Set<String> batchCodes = new HashSet<>();
        Set<String> countedCodes = new HashSet<>();
        AtomicInteger created = new AtomicInteger();
        AtomicInteger updated = new AtomicInteger();
        AtomicInteger processed = new AtomicInteger();

        Map<String, List<PendingChild>> waitingChildren = new HashMap<>();
        Map<String, PendingChild> unresolvedChildren = new HashMap<>();

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

                ensureSize(currentRow, 4);
                String rawCode = currentRow.get(0);
                String rawName = currentRow.get(1);
                String rawCfo = currentRow.get(2);
                String rawParent = currentRow.get(3);

                boolean allBlank = (isBlank(rawCode) && isBlank(rawName) && isBlank(rawCfo) && isBlank(rawParent));
                if (allBlank) {
                    return;
                }
                if (isBlank(rawCode)) {
                    throw new ExcelImportException("Строка " + (rowNum + 1) + ": не указан код БДЗ");
                }
                if (isBlank(rawName)) {
                    throw new ExcelImportException("Строка " + (rowNum + 1) + ": не указано наименование БДЗ");
                }

                String code = rawCode.trim();
                String name = rawName.trim();
                String key = normalizeCodeKey(code);
                Bdz existing = existingByCode.get(key);
                Bdz target = existing != null ? existing : new Bdz();
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

                removePendingLink(key, waitingChildren, unresolvedChildren);

                String parentCode = rawParent != null ? rawParent.trim() : "";
                if (!parentCode.isBlank()) {
                    String parentKey = normalizeCodeKey(parentCode);
                    if (parentKey.equals(key)) {
                        throw new ExcelImportException("Строка " + (rowNum + 1) + ": код родителя совпадает с кодом записи");
                    }
                    Bdz parent = existingByCode.get(parentKey);
                    if (parent != null) {
                        target.setParent(parent);
                    } else {
                        PendingChild pending = new PendingChild(target, key, rowNum + 1, parentCode, parentKey);
                        waitingChildren.computeIfAbsent(parentKey, k -> new ArrayList<>()).add(pending);
                        unresolvedChildren.put(key, pending);
                        target.setParent(null);
                    }
                } else {
                    target.setParent(null);
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

                scheduleForSave(target, key, batch, batchCodes);

                List<PendingChild> resolved = waitingChildren.remove(key);
                if (resolved != null) {
                    for (PendingChild pending : resolved) {
                        Bdz child = pending.child();
                        child.setParent(target);
                        unresolvedChildren.remove(pending.childKey());
                        scheduleForSave(child, pending.childKey(), batch, batchCodes);
                    }
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

        if (!unresolvedChildren.isEmpty()) {
            PendingChild pending = unresolvedChildren.values().iterator().next();
            throw new ExcelImportException(String.format(
                    "Строка %d: указан родитель с кодом '%s' для БДЗ с кодом '%s', но запись с таким кодом не найдена",
                    pending.rowNumber(), pending.parentCode(), pending.child().getCode()));
        }

        flushBatch(batch, batchCodes, existingByCode);
        return new ExcelImportResult(created.get(), updated.get(), processed.get());
    }

    private void scheduleForSave(Bdz entity, String key, List<Bdz> batch, Set<String> batchCodes) {
        if (key != null && batchCodes.add(key)) {
            batch.add(entity);
        }
    }

    private void flushBatch(List<Bdz> batch, Set<String> batchCodes, Map<String, Bdz> cache) {
        if (batch.isEmpty()) {
            return;
        }
        List<Bdz> saved = bdzRepository.saveAll(batch);
        for (Bdz savedEntity : saved) {
            if (savedEntity.getCode() != null) {
                cache.put(normalizeCodeKey(savedEntity.getCode()), savedEntity);
            }
        }
        batch.clear();
        batchCodes.clear();
    }

    private void validateHeader(List<String> header) {
        ensureSize(header, 4);
        String codeHeader = header.get(0) != null ? header.get(0).trim() : "";
        String nameHeader = header.get(1) != null ? header.get(1).trim() : "";
        String cfoHeader = header.get(2) != null ? header.get(2).trim() : "";
        String parentHeader = header.get(3) != null ? header.get(3).trim() : "";
        if (!"Код".equalsIgnoreCase(codeHeader)
                || !"Наименование".equalsIgnoreCase(nameHeader)
                || !"Код ЦФО I".equalsIgnoreCase(cfoHeader)
                || !"Код родителя".equalsIgnoreCase(parentHeader)) {
            throw new ExcelImportException("Некорректный заголовок файла. Ожидаются столбцы 'Код', 'Наименование', 'Код ЦФО I', 'Код родителя'.");
        }
    }

    private void removePendingLink(String childKey,
                                   Map<String, List<PendingChild>> waitingChildren,
                                   Map<String, PendingChild> unresolvedChildren) {
        PendingChild pending = unresolvedChildren.remove(childKey);
        if (pending != null) {
            List<PendingChild> siblings = waitingChildren.get(pending.parentKey());
            if (siblings != null) {
                siblings.removeIf(item -> item.childKey().equals(childKey));
                if (siblings.isEmpty()) {
                    waitingChildren.remove(pending.parentKey());
                }
            }
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

    private record PendingChild(Bdz child, String childKey, int rowNumber, String parentCode, String parentKey) {
    }
}
