package com.example.budget.service;

import com.example.budget.domain.Bdz;
import com.example.budget.repo.BdzRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.Deque;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

@Service
public class BdzImportService {

    private static final int MAX_ROWS = 10_000;

    private final BdzRepository bdzRepository;

    public BdzImportService(BdzRepository bdzRepository) {
        this.bdzRepository = bdzRepository;
    }

    @Transactional
    public ImportResult importFromExcel(InputStream inputStream) {
        Objects.requireNonNull(inputStream, "inputStream");
        List<BdzRow> rows = new XlsxBdzReader().readRows(inputStream);
        if (rows.isEmpty()) {
            throw new BdzImportException("Файл не содержит строк с данными.");
        }

        Map<String, Integer> rowNumbers = new HashMap<>();
        for (BdzRow row : rows) {
            Integer previous = rowNumbers.put(row.code(), row.rowNumber());
            if (previous != null) {
                throw new BdzImportException(String.format(
                        "Код '%s' дублируется (строки %d и %d).",
                        row.code(), previous, row.rowNumber()));
            }
        }

        Map<String, String> parentByCode = new HashMap<>();
        Set<String> parentCodes = new HashSet<>();
        for (BdzRow row : rows) {
            parentByCode.put(row.code(), row.parentCode());
            if (row.parentCode() != null) {
                parentCodes.add(row.parentCode());
            }
        }

        Set<String> importCodes = new HashSet<>(rowNumbers.keySet());
        Set<String> lookupCodes = new HashSet<>(importCodes);
        lookupCodes.addAll(parentCodes);

        Map<String, Bdz> existingByCode = findExistingByCode(lookupCodes);
        Set<String> missingParents = new HashSet<>();
        for (BdzRow row : rows) {
            String parentCode = row.parentCode();
            if (parentCode == null) {
                continue;
            }
            if (!importCodes.contains(parentCode) && !existingByCode.containsKey(parentCode)) {
                missingParents.add(parentCode);
            }
        }
        if (!missingParents.isEmpty()) {
            String missing = missingParents.iterator().next();
            int row = rows.stream()
                    .filter(r -> missing.equals(r.parentCode()))
                    .map(BdzRow::rowNumber)
                    .findFirst().orElse(-1);
            throw new BdzImportException(String.format(
                    "Строка %d: код родителя '%s' отсутствует в базе и в файле импорта.",
                    row, missing));
        }

        Map<String, Integer> depthCache = new HashMap<>();
        for (BdzRow row : rows) {
            computeDepth(row.code(), parentByCode, importCodes, rowNumbers, depthCache,
                    new HashSet<>(), new ArrayDeque<>());
        }

        List<BdzRow> orderedRows = new ArrayList<>(rows);
        orderedRows.sort(Comparator
                .comparingInt((BdzRow r) -> depthCache.getOrDefault(r.code(), 0))
                .thenComparingInt(BdzRow::rowNumber));

        Map<String, Bdz> entitiesByCode = new HashMap<>(existingByCode);
        List<Bdz> toPersist = new ArrayList<>();
        int created = 0;
        int updated = 0;

        for (BdzRow row : orderedRows) {
            Bdz entity = entitiesByCode.get(row.code());
            boolean isNew = false;
            if (entity == null) {
                entity = new Bdz();
                entity.setCode(row.code());
                entitiesByCode.put(row.code(), entity);
                isNew = true;
            }
            entity.setName(row.name());
            String parentCode = row.parentCode();
            Bdz parent = null;
            if (parentCode != null) {
                parent = entitiesByCode.get(parentCode);
                if (parent == null) {
                    parent = existingByCode.get(parentCode);
                }
            }
            entity.setParent(parent);
            toPersist.add(entity);
            if (isNew) {
                created++;
            } else {
                updated++;
            }
        }

        if (!toPersist.isEmpty()) {
            bdzRepository.saveAll(toPersist);
            bdzRepository.flush();
        }

        return new ImportResult(created, updated, orderedRows.size());
    }

    private Map<String, Bdz> findExistingByCode(Collection<String> codes) {
        if (codes.isEmpty()) {
            return new HashMap<>();
        }
        List<Bdz> list = bdzRepository.findByCodeIn(codes);
        Map<String, Bdz> map = new HashMap<>();
        for (Bdz item : list) {
            if (item.getCode() != null) {
                map.put(item.getCode(), item);
            }
        }
        return map;
    }

    private int computeDepth(String code,
                             Map<String, String> parentByCode,
                             Set<String> importCodes,
                             Map<String, Integer> rowNumbers,
                             Map<String, Integer> cache,
                             Set<String> visiting,
                             Deque<String> path) {
        Integer cached = cache.get(code);
        if (cached != null) {
            return cached;
        }
        if (!visiting.add(code)) {
            path.addLast(code);
            throw cycleException(path, rowNumbers, code);
        }
        path.addLast(code);
        String parent = parentByCode.get(code);
        int depth;
        if (parent == null || parent.isBlank() || !importCodes.contains(parent)) {
            depth = 0;
        } else {
            depth = computeDepth(parent, parentByCode, importCodes, rowNumbers, cache, visiting, path) + 1;
        }
        path.removeLast();
        visiting.remove(code);
        cache.put(code, depth);
        return depth;
    }

    private RuntimeException cycleException(Deque<String> path, Map<String, Integer> rowNumbers, String repeating) {
        StringBuilder sb = new StringBuilder("Обнаружена циклическая ссылка: ");
        boolean first = true;
        for (String code : path) {
            if (!first) {
                sb.append(" -> ");
            }
            first = false;
            sb.append(code);
            Integer row = rowNumbers.get(code);
            if (row != null) {
                sb.append(" (строка ").append(row).append(')');
            }
        }
        if (!path.isEmpty()) {
            sb.append(" -> ").append(repeating);
            Integer row = rowNumbers.get(repeating);
            if (row != null) {
                sb.append(" (строка ").append(row).append(')');
            }
        }
        return new BdzImportException(sb.toString());
    }

    public record ImportResult(int created, int updated, int total) { }

    private record BdzRow(int rowNumber, String code, String name, String parentCode) { }

    private static final class XlsxBdzReader {

        private final XMLInputFactory factory;

        private XlsxBdzReader() {
            factory = XMLInputFactory.newFactory();
            factory.setProperty(XMLInputFactory.IS_NAMESPACE_AWARE, false);
        }

        private List<BdzRow> readRows(InputStream inputStream) {
            try {
                return doReadRows(inputStream);
            } catch (IOException e) {
                throw new UncheckedIOException("Не удалось прочитать Excel-файл", e);
            }
        }

        private List<BdzRow> doReadRows(InputStream inputStream) throws IOException {
            List<String> sharedStrings = new ArrayList<>();
            Path sheetFile = null;
            boolean sheetCaptured = false;
            boolean sharedStringsLoaded = false;

            try (ZipInputStream zis = new ZipInputStream(inputStream)) {
                ZipEntry entry;
                while ((entry = zis.getNextEntry()) != null) {
                    String name = entry.getName();
                    if ("xl/sharedStrings.xml".equals(name)) {
                        parseSharedStrings(new NonClosingInputStream(zis), sharedStrings);
                        sharedStringsLoaded = true;
                    } else if (isWorksheet(name) && !sheetCaptured) {
                        sheetFile = Files.createTempFile("bdz-import", ".xml");
                        copyEntry(zis, sheetFile);
                        sheetCaptured = true;
                    }
                    zis.closeEntry();
                    if (sheetCaptured && sharedStringsLoaded) {
                        break;
                    }
                }
            } catch (XMLStreamException e) {
                throw new IOException("Ошибка разбора XML", e);
            }

            if (!sheetCaptured || sheetFile == null) {
                throw new BdzImportException("В файле не найден лист с данными.");
            }

            try (InputStream sheetStream = Files.newInputStream(sheetFile)) {
                return parseSheet(sheetStream, sharedStrings);
            } catch (XMLStreamException e) {
                throw new IOException("Ошибка разбора листа Excel", e);
            } finally {
                Files.deleteIfExists(sheetFile);
            }
        }

        private boolean isWorksheet(String name) {
            return name.startsWith("xl/worksheets/") && name.endsWith(".xml");
        }

        private void copyEntry(InputStream in, Path file) throws IOException {
            try (var out = Files.newOutputStream(file)) {
                in.transferTo(out);
            }
        }

        private void parseSharedStrings(InputStream in, List<String> target) throws XMLStreamException {
            XMLStreamReader reader = factory.createXMLStreamReader(in);
            StringBuilder current = null;
            boolean inText = false;
            while (reader.hasNext()) {
                int event = reader.next();
                if (event == XMLStreamConstants.START_ELEMENT) {
                    String local = reader.getLocalName();
                    if ("si".equals(local)) {
                        current = new StringBuilder();
                    } else if ("t".equals(local) && current != null) {
                        inText = true;
                    }
                } else if (event == XMLStreamConstants.CHARACTERS) {
                    if (inText && current != null) {
                        current.append(reader.getText());
                    }
                } else if (event == XMLStreamConstants.END_ELEMENT) {
                    String local = reader.getLocalName();
                    if ("t".equals(local)) {
                        inText = false;
                    } else if ("si".equals(local) && current != null) {
                        target.add(current.toString());
                        current = null;
                    }
                }
            }
            reader.close();
        }

        private List<BdzRow> parseSheet(InputStream in, List<String> sharedStrings) throws XMLStreamException, IOException {
            XMLStreamReader reader = factory.createXMLStreamReader(in);
            Map<Integer, String> currentRow = null;
            int currentRowNumber = 0;
            String currentType = null;
            int currentColumn = -1;
            StringBuilder valueBuilder = null;
            boolean inlineText = false;
            EnumMap<Column, Integer> columnIndexes = null;
            List<BdzRow> rows = new ArrayList<>();
            int dataRows = 0;

            try {
                while (reader.hasNext()) {
                    int event = reader.next();
                    switch (event) {
                        case XMLStreamConstants.START_ELEMENT -> {
                            String local = reader.getLocalName();
                            if ("row".equals(local)) {
                                currentRow = new HashMap<>();
                                String attr = reader.getAttributeValue(null, "r");
                                if (attr != null) {
                                    try {
                                        currentRowNumber = Integer.parseInt(attr);
                                    } catch (NumberFormatException ex) {
                                        currentRowNumber++;
                                    }
                                } else {
                                    currentRowNumber++;
                                }
                            } else if ("c".equals(local)) {
                                String ref = reader.getAttributeValue(null, "r");
                                currentColumn = ref != null ? columnIndex(ref) : -1;
                                currentType = reader.getAttributeValue(null, "t");
                            } else if ("v".equals(local)) {
                                valueBuilder = new StringBuilder();
                                inlineText = false;
                            } else if ("t".equals(local) && "inlineStr".equals(currentType)) {
                                valueBuilder = valueBuilder == null ? new StringBuilder() : valueBuilder;
                                inlineText = true;
                            }
                        }
                        case XMLStreamConstants.CHARACTERS -> {
                            if (valueBuilder != null) {
                                valueBuilder.append(reader.getText());
                            }
                        }
                        case XMLStreamConstants.END_ELEMENT -> {
                            String local = reader.getLocalName();
                            if ("v".equals(local)) {
                                if (currentRow != null && currentColumn >= 0) {
                                    String raw = valueBuilder != null ? valueBuilder.toString() : null;
                                    String text = resolveCellValue(raw, currentType, sharedStrings);
                                    currentRow.put(currentColumn, text);
                                }
                                valueBuilder = null;
                                inlineText = false;
                            } else if ("t".equals(local) && inlineText) {
                                if (currentRow != null && currentColumn >= 0) {
                                    String text = valueBuilder != null ? valueBuilder.toString() : null;
                                    currentRow.put(currentColumn, text);
                                }
                                valueBuilder = null;
                                inlineText = false;
                            } else if ("c".equals(local)) {
                                currentColumn = -1;
                                currentType = null;
                            } else if ("row".equals(local)) {
                                if (currentRow != null) {
                                    if (columnIndexes == null) {
                                        columnIndexes = resolveHeader(currentRow, currentRowNumber);
                                    } else {
                                        BdzRow row = convertRow(currentRow, currentRowNumber, columnIndexes);
                                        if (row != null) {
                                            rows.add(row);
                                            dataRows++;
                                            if (dataRows > MAX_ROWS) {
                                                throw new BdzImportException("Превышено допустимое количество строк (максимум 10000).");
                                            }
                                        }
                                    }
                                }
                                currentRow = null;
                            }
                        }
                    }
                }
            } finally {
                reader.close();
            }

            if (columnIndexes == null) {
                throw new BdzImportException("В файле не найдена строка заголовков.");
            }
            return rows;
        }

        private EnumMap<Column, Integer> resolveHeader(Map<Integer, String> row, int rowNumber) {
            EnumMap<Column, Integer> map = new EnumMap<>(Column.class);
            for (Map.Entry<Integer, String> entry : row.entrySet()) {
                String normalized = normalize(entry.getValue());
                if (normalized == null) {
                    continue;
                }
                switch (normalized) {
                    case "код" -> map.put(Column.CODE, entry.getKey());
                    case "наименование" -> map.put(Column.NAME, entry.getKey());
                    case "родитель" -> map.put(Column.PARENT, entry.getKey());
                }
            }
            if (!map.containsKey(Column.CODE) || !map.containsKey(Column.NAME)) {
                throw new BdzImportException(String.format(
                        "Строка %d: не найдены обязательные столбцы 'Код' и 'Наименование'.",
                        rowNumber));
            }
            return map;
        }

        private BdzRow convertRow(Map<Integer, String> row,
                                  int rowNumber,
                                  EnumMap<Column, Integer> indexes) {
            String code = trimToNull(row.get(indexes.get(Column.CODE)));
            String name = trimToNull(row.get(indexes.get(Column.NAME)));
            String parent = null;
            Integer parentIdx = indexes.get(Column.PARENT);
            if (parentIdx != null) {
                parent = trimToNull(row.get(parentIdx));
            }

            if (code == null && name == null && parent == null) {
                return null;
            }
            if (code == null) {
                throw new BdzImportException(String.format("Строка %d: не заполнен код.", rowNumber));
            }
            if (name == null) {
                throw new BdzImportException(String.format("Строка %d: не заполнено наименование.", rowNumber));
            }
            if (parent != null && parent.equals(code)) {
                throw new BdzImportException(String.format(
                        "Строка %d: код не может совпадать с кодом родителя.", rowNumber));
            }
            return new BdzRow(rowNumber, code, name, parent);
        }

        private String resolveCellValue(String raw, String type, List<String> sharedStrings) {
            if (raw == null) {
                return null;
            }
            if ("s".equals(type)) {
                try {
                    int index = Integer.parseInt(raw);
                    if (index >= 0 && index < sharedStrings.size()) {
                        return sharedStrings.get(index);
                    }
                } catch (NumberFormatException ignored) {
                }
                throw new BdzImportException("Некорректный индекс в sharedStrings: " + raw);
            }
            return raw;
        }

        private int columnIndex(String ref) {
            int result = 0;
            for (int i = 0; i < ref.length(); i++) {
                char ch = ref.charAt(i);
                if (Character.isDigit(ch)) {
                    break;
                }
                result = result * 26 + (ch - 'A' + 1);
            }
            return result - 1;
        }

        private String normalize(String value) {
            String trimmed = trimToNull(value);
            return trimmed != null ? trimmed.toLowerCase(Locale.ROOT) : null;
        }

        private String trimToNull(String value) {
            if (value == null) {
                return null;
            }
            String trimmed = value.trim();
            return trimmed.isEmpty() ? null : trimmed;
        }

        private static final class NonClosingInputStream extends FilterInputStream {
            private NonClosingInputStream(InputStream in) {
                super(in);
            }

            @Override
            public void close() {
                // игнорировать закрытие, чтобы не закрывать ZipInputStream
            }
        }

        private enum Column {
            CODE, NAME, PARENT
        }
    }
}
