package com.example.backend.service;

import com.example.backend.dto.AdReportRequest;
import com.example.backend.entity.AdReport;
import com.example.backend.util.PdfGenerator;
import org.springframework.stereotype.Service;

@Service
public class ReportService {

    private final PdfGenerator pdfGenerator;

    public ReportService(PdfGenerator pdfGenerator) {
        this.pdfGenerator = pdfGenerator;
    }

    public void createReport(AdReportRequest request) {
        AdReport report = new AdReport(
                request.getAdId(),
                request.getAdName(),
                request.getUserId(),
                request.getAdGoal(),
                request.getPerformanceGoal(),
                request.getResultRate(),
                request.getPostChangeRate()
        );

        pdfGenerator.generate(report);
        printToConsole(report);
    }

    private void printToConsole(AdReport report) {
        System.out.println("========= 광고 리포트 요약 =========");
        System.out.println("광고 ID: " + report.getAdId());
        System.out.println("광고명: " + report.getAdName());
        System.out.println("사용자 ID: " + report.getUserId());
        System.out.println("성과 목표: " + report.getPerformanceGoal());
        System.out.printf("현재 성과: %.2f%%\n", report.getResultRate());
        System.out.printf("교체 후 성과: %.2f%%\n", report.getPostChangeRate());
        System.out.printf("향상률: %.2f%%\n", report.getPostChangeRate() - report.getResultRate());
        System.out.println("===================================");
    }
}
