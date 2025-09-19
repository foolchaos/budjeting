package com.example.budget.service;

import com.example.budget.domain.Bdz;
import com.example.budget.domain.Bo;
import com.example.budget.domain.Cfo;
import com.example.budget.domain.CfoTwo;
import com.example.budget.domain.Contract;
import com.example.budget.domain.ContractAmount;
import com.example.budget.domain.Counterparty;
import com.example.budget.domain.Mvz;
import com.example.budget.domain.Request;
import com.example.budget.domain.RequestPosition;
import com.example.budget.domain.Zgd;
import org.apache.poi.ss.usermodel.BorderStyle;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
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

            CellStyle titleStyle = createTitleStyle(workbook);

            Row cfoRow = sheet.createRow(rowIndex++);
            Cfo cfo = request.getCfo();
            String cfoCode = cfo != null ? safeTrim(cfo.getCode()) : "";
            String cfoName = cfo != null ? safeTrim(cfo.getName()) : "";
            fillTitleRow(sheet, cfoRow, "ЦФО I", cfoCode, cfoName, titleStyle);

            Row requestInfoRow = sheet.createRow(rowIndex++);
            String requestName = safeTrim(request.getName());
            String yearValue = safeTrim(request.getYear() != null ? request.getYear().toString() : null);
            fillTitleRow(sheet, requestInfoRow, "Заявка", yearValue, requestName, titleStyle);

            String[] headers = {
                    "№",
                    "ЦФО II",
                    "МВЗ",
                    "Статья БДЗ",
                    "Статья БО",
                    "Ф.И.О. курирующего ЗГД/ГД",
                    "ВГО",
                    "Контрагент",
                    "№ договора",
                    "Системный № карточки из ИУС ПД",
                    "Ответственный по договору (Ф.И.О.)",
                    "Предмет договора",
                    "Период",
                    "Сумма/млн. руб. (без НДС)",
                    "Затраты связанные с вводными объектами, в млн.руб. (без НДС)"
            };

            CellStyle headerStyle = createHeaderStyle(workbook);
            CellStyle dataStyle = createDataStyle(workbook);
            CellStyle numericAmountStyle = createNumericAmountStyle(workbook);
            Row headerRow = sheet.createRow(rowIndex++);
            for (int i = 0; i < headers.length; i++) {
                setStringCell(headerRow, i, headers[i], headerStyle);
            }

            int order = 1;
            for (RequestPosition position : positions) {
                Row row = sheet.createRow(rowIndex++);
                Cell numberCell = row.createCell(0);
                numberCell.setCellValue(order++);
                numberCell.setCellStyle(dataStyle);
                setStringCell(row, 1, formatCodeAndName(position.getCfo2()), dataStyle);
                setStringCell(row, 2, formatCodeAndName(position.getMvz()), dataStyle);
                setStringCell(row, 3, formatCodeAndName(position.getBdz()), dataStyle);
                setStringCell(row, 4, formatCodeAndName(position.getBo()), dataStyle);
                setStringCell(row, 5, formatZgd(position.getZgd()), dataStyle);
                setStringCell(row, 6, safeString(position.getVgo()), dataStyle);
                setStringCell(row, 7, formatCounterparty(position.getCounterparty()), dataStyle);
                setStringCell(row, 8, extractExternalContractNumber(position.getContract()), dataStyle);
                setStringCell(row, 9, extractInternalContractNumber(position.getContract()), dataStyle);
                setStringCell(row, 10, extractContractResponsible(position.getContract()), dataStyle);
                setStringCell(row, 11, safeString(position.getSubject()), dataStyle);
                setStringCell(row, 12, safeString(position.getPeriod()), dataStyle);
                setAmountCell(row, 13, position.getAmountNoVat(), numericAmountStyle);
                setAmountCell(row, 14, resolveInputObjectAmount(position), numericAmountStyle);
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
        applyAllBorders(style);
        return style;
    }

    private CellStyle createTitleStyle(Workbook workbook) {
        Font font = workbook.createFont();
        font.setBold(true);
        font.setFontHeightInPoints((short) 14);
        CellStyle style = workbook.createCellStyle();
        style.setFont(font);
        return style;
    }

    private CellStyle createDataStyle(Workbook workbook) {
        CellStyle style = workbook.createCellStyle();
        applyAllBorders(style);
        return style;
    }

    private CellStyle createNumericAmountStyle(Workbook workbook) {
        CellStyle style = workbook.createCellStyle();
        applyAllBorders(style);
        style.setDataFormat(workbook.createDataFormat().getFormat("# ##0.000"));
        return style;
    }

    private void applyAllBorders(CellStyle style) {
        style.setBorderTop(BorderStyle.THIN);
        style.setBorderBottom(BorderStyle.THIN);
        style.setBorderLeft(BorderStyle.THIN);
        style.setBorderRight(BorderStyle.THIN);
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

    private void setAmountCell(Row row, int columnIndex, BigDecimal value, CellStyle style) {
        Cell cell = row.createCell(columnIndex);
        if (style != null) {
            cell.setCellStyle(style);
        }
        if (value != null) {
            BigDecimal scaledValue = value.setScale(3, RoundingMode.HALF_UP);
            cell.setCellValue(scaledValue.doubleValue());
        }
    }

    private void fillTitleRow(Sheet sheet, Row row, String label, String middleValue, String mergedValue, CellStyle style) {
        if (style != null) {
            row.setHeightInPoints(20f);
        }
        setStringCell(row, 0, label, style);
        setStringCell(row, 1, "", style);
        mergeCells(sheet, row.getRowNum(), 0, 1);

        setStringCell(row, 2, middleValue, style);

        setStringCell(row, 3, mergedValue, style);
        setStringCell(row, 4, "", style);
        mergeCells(sheet, row.getRowNum(), 3, 4);
    }

    private void mergeCells(Sheet sheet, int rowIndex, int firstColumn, int lastColumn) {
        sheet.addMergedRegion(new CellRangeAddress(rowIndex, rowIndex, firstColumn, lastColumn));
    }

    private BigDecimal resolveInputObjectAmount(RequestPosition position) {
        if (position == null || !position.isInputObject()) {
            return null;
        }
        ContractAmount contractAmount = position.getContractAmount();
        if (contractAmount != null && contractAmount.getAmount() != null) {
            return contractAmount.getAmount();
        }
        return position.getAmount();
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
        return joinWithSpace(code, name);
    }

    private String joinWithSpace(String first, String second) {
        boolean hasFirst = hasText(first);
        boolean hasSecond = hasText(second);
        if (!hasFirst && !hasSecond) {
            return "";
        }
        if (hasFirst && hasSecond) {
            return first.trim() + " " + second.trim();
        }
        return hasFirst ? first.trim() : second.trim();
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

    private String extractContractResponsible(Contract contract) {
        return contract != null ? safeString(contract.getResponsible()) : "";
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

    private String safeTrim(String value) {
        return value != null ? value.trim() : "";
    }

    private boolean hasText(String value) {
        return value != null && !value.trim().isEmpty();
    }
}
