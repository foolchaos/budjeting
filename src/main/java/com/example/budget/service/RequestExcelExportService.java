package com.example.budget.service;

import com.example.budget.domain.Bdz;
import com.example.budget.domain.Bo;
import com.example.budget.domain.Cfo;
import com.example.budget.domain.CfoTwo;
import com.example.budget.domain.Contract;
import com.example.budget.domain.Counterparty;
import com.example.budget.domain.Mvz;
import com.example.budget.domain.Request;
import com.example.budget.domain.RequestPosition;
import com.example.budget.domain.Zgd;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.util.List;

@Service
public class RequestExcelExportService {
    private static final String DEFAULT_FILE_NAME = "zayavka";
    private static final String FILE_EXTENSION = ".xlsx";

    private final RequestService requestService;
    private final RequestPositionService requestPositionService;

    public RequestExcelExportService(RequestService requestService, RequestPositionService requestPositionService) {
        this.requestService = requestService;
        this.requestPositionService = requestPositionService;
    }

    public RequestExcelExportResult exportRequest(Long requestId) {
        if (requestId == null) {
            throw new IllegalArgumentException("Request id must not be null");
        }
        Request request = requestService.findById(requestId);
        List<RequestPosition> positions = requestPositionService.findByRequestId(requestId);
        byte[] content = buildWorkbook(request, positions);
        String fileName = buildFileName(request);
        return new RequestExcelExportResult(fileName, content);
    }

    private byte[] buildWorkbook(Request request, List<RequestPosition> positions) {
        try (Workbook workbook = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Sheet sheet = workbook.createSheet("Заявка");
            int rowIndex = 0;

            Row cfoRow = sheet.createRow(rowIndex++);
            setStringCell(cfoRow, 0, formatCodeAndName(request.getCfo()));

            Row requestInfoRow = sheet.createRow(rowIndex++);
            String requestName = safeString(request.getName());
            Integer requestYear = request.getYear();
            String yearValue = requestYear != null ? requestYear.toString() : "";
            String requestInfo = requestName;
            if (hasText(yearValue)) {
                requestInfo = hasText(requestName) ? requestName + " " + yearValue : yearValue;
            }
            setStringCell(requestInfoRow, 0, requestInfo);

            String[] headers = {
                    "№",
                    "Статья БДЗ",
                    "ЦФО II",
                    "МВЗ",
                    "ВГО",
                    "Статья БО",
                    "Контрагент",
                    "№ договора",
                    "Системный № карточки из ИУС ПД",
                    "Способ закупки",
                    "Ф.И.О. курирующего ЗГД/ГД",
                    "Предмет договора",
                    "Период",
                    "Затраты связанные с вводными объектами, в млн.руб. (без НДС)",
                    "СУММА, в млн.руб."
            };

            CellStyle headerStyle = createHeaderStyle(workbook);
            Row headerRow = sheet.createRow(rowIndex++);
            for (int i = 0; i < headers.length; i++) {
                setStringCell(headerRow, i, headers[i], headerStyle);
            }

            int order = 1;
            for (RequestPosition position : positions) {
                Row row = sheet.createRow(rowIndex++);
                row.createCell(0).setCellValue(order++);
                setStringCell(row, 1, formatCodeAndName(position.getBdz()));
                setStringCell(row, 2, formatCodeAndName(position.getCfo2()));
                setStringCell(row, 3, formatCodeAndName(position.getMvz()));
                setStringCell(row, 4, safeString(position.getVgo()));
                setStringCell(row, 5, formatCodeAndName(position.getBo()));
                setStringCell(row, 6, formatCounterparty(position.getCounterparty()));
                setStringCell(row, 7, extractExternalContractNumber(position.getContract()));
                setStringCell(row, 8, extractInternalContractNumber(position.getContract()));
                setStringCell(row, 9, safeString(position.getProcurementMethod()));
                setStringCell(row, 10, formatZgd(position.getZgd()));
                setStringCell(row, 11, safeString(position.getSubject()));
                setStringCell(row, 12, safeString(position.getPeriod()));
                setStringCell(row, 13, formatAmount(position.getAmountNoVat()));
                setStringCell(row, 14, position.isInputObject() ? formatAmount(position.getAmount()) : "");
            }

            for (int i = 0; i < headers.length; i++) {
                sheet.autoSizeColumn(i);
            }

            workbook.write(out);
            return out.toByteArray();
        } catch (IOException e) {
            throw new IllegalStateException("Failed to build Excel file", e);
        }
    }

    private CellStyle createHeaderStyle(Workbook workbook) {
        Font font = workbook.createFont();
        font.setBold(true);
        CellStyle style = workbook.createCellStyle();
        style.setFont(font);
        return style;
    }

    private void setStringCell(Row row, int columnIndex, String value) {
        setStringCell(row, columnIndex, value, null);
    }

    private void setStringCell(Row row, int columnIndex, String value, CellStyle style) {
        Cell cell = row.createCell(columnIndex);
        cell.setCellValue(value != null ? value : "");
        if (style != null) {
            cell.setCellStyle(style);
        }
    }

    private String formatCodeAndName(Bdz bdz) {
        if (bdz == null) {
            return "";
        }
        return formatCodeAndName(bdz.getCode(), bdz.getName());
    }

    private String formatCodeAndName(Bo bo) {
        if (bo == null) {
            return "";
        }
        return formatCodeAndName(bo.getCode(), bo.getName());
    }

    private String formatCodeAndName(CfoTwo cfoTwo) {
        if (cfoTwo == null) {
            return "";
        }
        return formatCodeAndName(cfoTwo.getCode(), cfoTwo.getName());
    }

    private String formatCodeAndName(Cfo cfo) {
        if (cfo == null) {
            return "";
        }
        return formatCodeAndName(cfo.getCode(), cfo.getName());
    }

    private String formatCodeAndName(Mvz mvz) {
        if (mvz == null) {
            return "";
        }
        return formatCodeAndName(mvz.getCode(), mvz.getName());
    }

    private String formatCodeAndName(String code, String name) {
        boolean hasCode = hasText(code);
        boolean hasName = hasText(name);
        if (!hasCode && !hasName) {
            return "";
        }
        if (hasCode && hasName) {
            return code.trim() + " " + name.trim();
        }
        return hasCode ? code.trim() : name.trim();
    }

    private String formatCounterparty(Counterparty counterparty) {
        return counterparty != null ? safeString(counterparty.getLegalEntityName()) : "";
    }

    private String extractExternalContractNumber(Contract contract) {
        return contract != null ? safeString(contract.getExternalNumber()) : "";
    }

    private String extractInternalContractNumber(Contract contract) {
        return contract != null ? safeString(contract.getInternalNumber()) : "";
    }

    private String formatZgd(Zgd zgd) {
        if (zgd == null) {
            return "";
        }
        String fullName = safeString(zgd.getFullName());
        String department = safeString(zgd.getDepartment());
        if (hasText(fullName) && hasText(department)) {
            return fullName + ", " + department;
        }
        return hasText(fullName) ? fullName : department;
    }

    private String formatAmount(BigDecimal value) {
        if (value == null) {
            return "";
        }
        return value.stripTrailingZeros().toPlainString();
    }

    private String buildFileName(Request request) {
        String base = safeString(request.getName());
        if (!hasText(base)) {
            base = DEFAULT_FILE_NAME;
        }
        base = sanitizeFileName(base);
        Integer year = request.getYear();
        String suffix = year != null ? year.toString() : "bez_goda";
        return base + "_" + suffix + FILE_EXTENSION;
    }

    private String sanitizeFileName(String value) {
        String sanitized = value.trim().replaceAll("\\s+", "_");
        sanitized = sanitized.replaceAll("[\\\\/:*?\"<>|]", "_");
        sanitized = sanitized.replaceAll("_+", "_");
        sanitized = sanitized.replaceAll("^_+", "");
        sanitized = sanitized.replaceAll("_+$", "");
        return hasText(sanitized) ? sanitized : DEFAULT_FILE_NAME;
    }

    private String safeString(String value) {
        return value != null ? value : "";
    }

    private boolean hasText(String value) {
        return value != null && !value.trim().isEmpty();
    }
}
