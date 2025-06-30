package com.example.backend.controller;

import com.example.backend.dto.AdReportRequest;
import com.example.backend.service.ReportService;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/report")
public class ReportController {

    private final ReportService reportService;

    public ReportController(ReportService reportService) {
        this.reportService = reportService;
    }

    @PostMapping
    public String generateReport(@RequestBody AdReportRequest request) {
        reportService.createReport(request);
        return "리포트 생성 완료";
    }
}