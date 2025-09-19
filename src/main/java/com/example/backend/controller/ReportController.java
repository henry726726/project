package com.example.backend.controller;

import com.example.backend.service.ReportService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;

@RestController
public class ReportController {

    @Autowired
    private ReportService reportService;

    @GetMapping("/api/report/pdf")
    public ResponseEntity<byte[]> generatePdfReport() throws IOException {
        byte[] pdfBytes = reportService.generateAdPerformancePdfBytes();

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=Ad_Performance_Report.pdf")
                .contentType(MediaType.APPLICATION_PDF)
                .body(pdfBytes);
    }
}