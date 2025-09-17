package com.example.budget.service;

import com.example.budget.domain.Bdz;
import com.example.budget.domain.RequestPosition;
import com.example.budget.domain.Zgd;
import com.example.budget.repo.BdzRepository;
import com.example.budget.repo.RequestPositionRepository;
import com.example.budget.repo.ZgdRepository;
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
public class ZgdService {

    private static final long MAX_IMPORT_FILE_SIZE = 10 * 1024 * 1024L;
    private static final int MAX_IMPORT_ROWS = 50_000;
    private static final int IMPORT_BATCH_SIZE = 200;

    private final ZgdRepository zgdRepository;
    private final RequestPositionRepository requestRepository;
    private final BdzRepository bdzRepository;

    public ZgdService(ZgdRepository zgdRepository, RequestPositionRepository requestRepository, BdzRepository bdzRepository) {
        this.zgdRepository = zgdRepository;
        this.requestRepository = requestRepository;
        this.bdzRepository = bdzRepository;
    }

    @Transactional(readOnly = true)
    public List<Zgd> findAll() {
        return zgdRepository.findAll();
    }

    public Zgd save(Zgd zgd) {
        return zgdRepository.save(zgd);
    }

    @Transactional
    public void delete(Zgd zgd) {
        if (zgd == null || zgd.getId() == null) {
            return;
        }
        deleteById(zgd.getId());
    }

    @Transactional
    public void deleteById(Long id) {
        if (id == null) {
            return;
        }

        Zgd managed = zgdRepository.findById(id).orElse(null);
        if (managed == null) {
            return;
        }

        List<RequestPosition> requests = requestRepository.findByZgdId(id);
        if (!requests.isEmpty()) {
            requests.forEach(r -> r.setZgd(null));
            requestRepository.saveAll(requests);
        }

        zgdRepository.delete(managed);
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
            Path temp = Files.createTempFile("zgd-import", ".xlsx");
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

        Map<String, Zgd> existingByKey = new HashMap<>();
        Map<Long, Zgd> bdzAssignments = new HashMap<>();
        for (Zgd zgd : zgdRepository.findAll()) {
            if (zgd.getFullName() != null && zgd.getDepartment() != null) {
                existingByKey.put(normalizeKey(zgd.getFullName(), zgd.getDepartment()), zgd);
            }
            if (zgd.getBdz() != null && zgd.getBdz().getId() != null) {
                bdzAssignments.put(zgd.getBdz().getId(), zgd);
            }
        }

        Map<String, Bdz> bdzByCode = new HashMap<>();
        for (Bdz bdz : bdzRepository.findAll()) {
            if (bdz.getCode() != null) {
                bdzByCode.put(normalizeCode(bdz.getCode()), bdz);
            }
        }

        List<Zgd> batch = new ArrayList<>();
        Set<String> batchKeys = new HashSet<>();
        Set<String> countedKeys = new HashSet<>();
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
                String rawFullName = currentRow.get(0);
                String rawDepartment = currentRow.get(1);
                String rawBdz = currentRow.get(2);

                boolean allBlank = isBlank(rawFullName) && isBlank(rawDepartment) && isBlank(rawBdz);
                if (allBlank) {
                    return;
                }
                if (isBlank(rawFullName)) {
                    throw new ExcelImportException("Строка " + (rowNum + 1) + ": не указано ФИО");
                }
                if (isBlank(rawDepartment)) {
                    throw new ExcelImportException("Строка " + (rowNum + 1) + ": не указан департамент");
                }

                String fullName = rawFullName.trim();
                String department = rawDepartment.trim();
                String key = normalizeKey(fullName, department);
                Zgd existing = existingByKey.get(key);
                Zgd target = existing != null ? existing : new Zgd();
                target.setFullName(fullName);
                target.setDepartment(department);
                existingByKey.put(key, target);

                Bdz newBdz = null;
                String bdzCode = rawBdz != null ? rawBdz.trim() : "";
                if (!bdzCode.isBlank()) {
                    String bdzKey = normalizeCode(bdzCode);
                    Bdz bdz = bdzByCode.get(bdzKey);
                    if (bdz == null) {
                        throw new ExcelImportException("Строка " + (rowNum + 1) + ": БДЗ с кодом '" + bdzCode + "' не найден");
                    }
                    newBdz = bdz;
                }

                Bdz previous = target.getBdz();
                if (previous != null && (newBdz == null || !previous.getId().equals(newBdz.getId()))) {
                    Zgd owner = bdzAssignments.get(previous.getId());
                    if (owner == target) {
                        bdzAssignments.remove(previous.getId());
                    }
                }

                if (newBdz != null) {
                    Zgd currentOwner = bdzAssignments.get(newBdz.getId());
                    if (currentOwner != null && currentOwner != target) {
                        currentOwner.setBdz(null);
                        String ownerKey = normalizeKey(currentOwner.getFullName(), currentOwner.getDepartment());
                        scheduleForSave(currentOwner, ownerKey, batch, batchKeys);
                    }
                    bdzAssignments.put(newBdz.getId(), target);
                }

                target.setBdz(newBdz);

                boolean firstOccurrence = countedKeys.add(key);
                if (firstOccurrence) {
                    if (existing == null) {
                        created.incrementAndGet();
                    } else {
                        updated.incrementAndGet();
                    }
                }
                processed.incrementAndGet();

                scheduleForSave(target, key, batch, batchKeys);

                if (batch.size() >= IMPORT_BATCH_SIZE) {
                    flushBatch(batch, batchKeys, existingByKey, bdzAssignments);
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

        flushBatch(batch, batchKeys, existingByKey, bdzAssignments);
        return new ExcelImportResult(created.get(), updated.get(), processed.get());
    }

    private void scheduleForSave(Zgd entity, String key, List<Zgd> batch, Set<String> batchKeys) {
        if (key != null && batchKeys.add(key)) {
            batch.add(entity);
        }
    }

    private void flushBatch(List<Zgd> batch,
                            Set<String> batchKeys,
                            Map<String, Zgd> cache,
                            Map<Long, Zgd> bdzAssignments) {
        if (batch.isEmpty()) {
            return;
        }
        List<Zgd> saved = zgdRepository.saveAll(batch);
        for (Zgd savedEntity : saved) {
            String key = normalizeKey(savedEntity.getFullName(), savedEntity.getDepartment());
            if (key != null) {
                cache.put(key, savedEntity);
            }
            if (savedEntity.getBdz() != null && savedEntity.getBdz().getId() != null) {
                bdzAssignments.put(savedEntity.getBdz().getId(), savedEntity);
            }
        }
        batch.clear();
        batchKeys.clear();
    }

    private void validateHeader(List<String> header) {
        ensureSize(header, 3);
        String fio = header.get(0) != null ? header.get(0).trim() : "";
        String department = header.get(1) != null ? header.get(1).trim() : "";
        String bdz = header.get(2) != null ? header.get(2).trim() : "";
        if (!"ФИО".equalsIgnoreCase(fio)
                || !"Департамент".equalsIgnoreCase(department)
                || !"Код БДЗ".equalsIgnoreCase(bdz)) {
            throw new ExcelImportException("Некорректный заголовок файла. Ожидаются столбцы 'ФИО', 'Департамент', 'Код БДЗ'.");
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

    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    private static String normalizeKey(String fullName, String department) {
        String normalizedName = normalize(fullName);
        String normalizedDepartment = normalize(department);
        if (normalizedName == null || normalizedDepartment == null) {
            return null;
        }
        return normalizedName + "|" + normalizedDepartment;
    }

    private static String normalize(String value) {
        return value == null ? null : value.trim().toUpperCase(Locale.ROOT);
    }

    private static String normalizeCode(String code) {
        return normalize(code);
    }
}
