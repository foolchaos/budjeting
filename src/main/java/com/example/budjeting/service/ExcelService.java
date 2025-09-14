package com.example.budjeting.service;

import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.streaming.SXSSFWorkbook;
import org.springframework.stereotype.Service;

@Service
public class ExcelService {
    public Workbook createWorkbook() {
        return new SXSSFWorkbook();
    }
}
