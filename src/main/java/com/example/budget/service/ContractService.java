package com.example.budget.service;

import com.example.budget.domain.Contract;
import com.example.budget.domain.Counterparty;
import com.example.budget.domain.RequestPosition;
import com.example.budget.repo.ContractRepository;
import com.example.budget.repo.CounterpartyRepository;
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
import java.util.List;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.format.ResolverStyle;

@Service
public class ContractService {

    private static final long MAX_IMPORT_FILE_SIZE = 10 * 1024 * 1024L;
    private static final int MAX_IMPORT_ROWS = 50_000;
    private static final int IMPORT_BATCH_SIZE = 200;

    private static final DateTimeFormatter[] DATE_FORMATTERS = new DateTimeFormatter[] {
            DateTimeFormatter.ISO_LOCAL_DATE,
            DateTimeFormatter.ofPattern("d.M.uuuu").withResolverStyle(ResolverStyle.STRICT),
            DateTimeFormatter.ofPattern("d/M/uuuu").withResolverStyle(ResolverStyle.STRICT)
    };

    private final ContractRepository contractRepository;
    private final RequestPositionRepository requestRepository;
    private final CounterpartyRepository counterpartyRepository;

    public ContractService(ContractRepository contractRepository,
                           RequestPositionRepository requestRepository,
                           CounterpartyRepository counterpartyRepository) {
        this.contractRepository = contractRepository;
        this.requestRepository = requestRepository;
        this.counterpartyRepository = counterpartyRepository;
    }

    @Transactional(readOnly = true)
    public List<Contract> findAll() {
        return contractRepository.findAll();
    }

    public Contract save(Contract contract) {
        if (contract.getCounterparty() != null && contract.getCounterparty().getId() != null) {
            contract.setCounterparty(counterpartyRepository
                    .findById(contract.getCounterparty().getId())
                    .orElseThrow(() -> new IllegalArgumentException(
                            "Counterparty not found: " + contract.getCounterparty().getId())));
        }
        return contractRepository.save(contract);
    }

    @Transactional
    public void delete(Contract contract) {
        if (contract == null || contract.getId() == null) {
            return;
        }
        deleteById(contract.getId());
    }

    @Transactional
    public void deleteById(Long id) {
        if (id == null) {
            return;
        }

        Contract managed = contractRepository.findById(id).orElse(null);
        if (managed == null) {
            return;
        }

        List<RequestPosition> requests = requestRepository.findByContractId(id);
        if (!requests.isEmpty()) {
            requests.forEach(r -> r.setContract(null));
            requestRepository.saveAll(requests);
        }

        contractRepository.delete(managed);
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
            Path temp = Files.createTempFile("contract-import", ".xlsx");
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

        Map<String, Contract> existingByInternal = new HashMap<>();
        for (Contract contract : contractRepository.findAll()) {
            if (!isBlank(contract.getInternalNumber())) {
                existingByInternal.put(normalizeKey(contract.getInternalNumber()), contract);
            }
        }

        Map<String, Counterparty> counterpartyByName = new HashMap<>();
        for (Counterparty counterparty : counterpartyRepository.findAll()) {
            if (!isBlank(counterparty.getLegalEntityName())) {
                counterpartyByName.put(normalizeCounterpartyKey(counterparty.getLegalEntityName()), counterparty);
            }
        }

        List<Contract> batch = new ArrayList<>();
        Set<String> batchKeys = new HashSet<>();
        Set<String> countedKeys = new HashSet<>();
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

                ensureSize(currentRow, 6);
                String rawName = currentRow.get(0);
                String rawInternal = currentRow.get(1);
                String rawExternal = currentRow.get(2);
                String rawDate = currentRow.get(3);
                String rawResponsible = currentRow.get(4);
                String rawCounterparty = currentRow.get(5);

                boolean allBlank = isBlank(rawName) && isBlank(rawInternal) && isBlank(rawExternal)
                        && isBlank(rawDate) && isBlank(rawResponsible) && isBlank(rawCounterparty);
                if (allBlank) {
                    return;
                }

                if (isBlank(rawInternal)) {
                    throw new ExcelImportException("Строка " + (rowNum + 1) + ": не указан № внутренний договора");
                }
                if (isBlank(rawName)) {
                    throw new ExcelImportException("Строка " + (rowNum + 1) + ": не указано наименование договора");
                }
                if (isBlank(rawDate)) {
                    throw new ExcelImportException("Строка " + (rowNum + 1) + ": не указана дата договора");
                }
                if (isBlank(rawResponsible)) {
                    throw new ExcelImportException("Строка " + (rowNum + 1) + ": не указан ответственный");
                }

                String internalNumber = rawInternal.trim();
                String key = normalizeKey(internalNumber);
                Contract existing = existingByInternal.get(key);
                Contract target = existing != null ? existing : new Contract();
                target.setInternalNumber(internalNumber);
                String name = rawName != null ? rawName.trim() : null;
                target.setName(name);
                String externalNumber = !isBlank(rawExternal) ? rawExternal.trim() : null;
                target.setExternalNumber(externalNumber);
                String responsible = rawResponsible != null ? rawResponsible.trim() : null;
                target.setContractDate(parseDate(rawDate, rowNum));
                target.setResponsible(responsible);

                String counterpartyName = rawCounterparty != null ? rawCounterparty.trim() : null;
                if (counterpartyName != null && !counterpartyName.isEmpty()) {
                    String counterpartyKey = normalizeCounterpartyKey(counterpartyName);
                    Counterparty counterparty = counterpartyByName.get(counterpartyKey);
                    if (counterparty == null) {
                        throw new ExcelImportException("Строка " + (rowNum + 1)
                                + ": контрагент '" + counterpartyName + "' не найден");
                    }
                    target.setCounterparty(counterparty);
                } else {
                    target.setCounterparty(null);
                }

                existingByInternal.put(key, target);

                boolean firstOccurrence = countedKeys.add(key);
                if (firstOccurrence) {
                    if (existing == null) {
                        created.incrementAndGet();
                    } else {
                        updated.incrementAndGet();
                    }
                }
                processed.incrementAndGet();

                if (batchKeys.add(key)) {
                    batch.add(target);
                }

                if (batch.size() >= IMPORT_BATCH_SIZE) {
                    flushBatch(batch, batchKeys, existingByInternal);
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

        flushBatch(batch, batchKeys, existingByInternal);
        return new ExcelImportResult(created.get(), updated.get(), processed.get());
    }

    private void flushBatch(List<Contract> batch, Set<String> batchKeys, Map<String, Contract> cache) {
        if (batch.isEmpty()) {
            return;
        }
        List<Contract> saved = contractRepository.saveAll(batch);
        for (Contract savedEntity : saved) {
            if (!isBlank(savedEntity.getInternalNumber())) {
                cache.put(normalizeKey(savedEntity.getInternalNumber()), savedEntity);
            }
        }
        batch.clear();
        batchKeys.clear();
    }

    private void validateHeader(List<String> header) {
        ensureSize(header, 6);
        String nameHeader = header.get(0) != null ? header.get(0).trim() : "";
        String internalHeader = header.get(1) != null ? header.get(1).trim() : "";
        String externalHeader = header.get(2) != null ? header.get(2).trim() : "";
        String dateHeader = header.get(3) != null ? header.get(3).trim() : "";
        String responsibleHeader = header.get(4) != null ? header.get(4).trim() : "";
        String counterpartyHeader = header.get(5) != null ? header.get(5).trim() : "";
        if (!"Наименование".equalsIgnoreCase(nameHeader)
                || !"№ внутренний".equalsIgnoreCase(internalHeader)
                || !"№ внешний".equalsIgnoreCase(externalHeader)
                || !"Дата".equalsIgnoreCase(dateHeader)
                || !"Ответственный".equalsIgnoreCase(responsibleHeader)
                || !"Контрагент".equalsIgnoreCase(counterpartyHeader)) {
            throw new ExcelImportException("Некорректный заголовок файла. Ожидаются столбцы 'Наименование', '№ внутренний', '№ внешний', 'Дата', 'Ответственный', 'Контрагент'.");
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

    private LocalDate parseDate(String rawValue, int rowNum) {
        String value = rawValue != null ? rawValue.trim() : "";
        for (DateTimeFormatter formatter : DATE_FORMATTERS) {
            try {
                return LocalDate.parse(value, formatter);
            } catch (DateTimeParseException ignored) {
            }
        }
        throw new ExcelImportException("Строка " + (rowNum + 1) + ": не удалось распознать дату договора '" + rawValue + "'");
    }

    private static String normalizeKey(String value) {
        return value == null ? null : value.trim().toUpperCase(Locale.ROOT);
    }

    private static String normalizeCounterpartyKey(String value) {
        return value == null ? null : value.trim().toUpperCase(Locale.ROOT);
    }

    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
