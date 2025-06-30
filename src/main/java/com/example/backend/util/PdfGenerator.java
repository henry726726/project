package com.example.backend.util;

import com.example.backend.entity.AdReport;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.font.PdfFont;
import com.itextpdf.kernel.font.PdfFontFactory;
import com.itextpdf.io.font.PdfEncodings;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.element.Paragraph;
import org.springframework.stereotype.Component;

import java.io.File;

@Component
public class PdfGenerator {

    public void generate(AdReport report) {
        try {
            String filename = "AdReport_" + report.getUserId() + "_" + report.getAdId() + ".pdf";
            PdfWriter writer = new PdfWriter(new File(filename));
            PdfDocument pdf = new PdfDocument(writer);
            Document doc = new Document(pdf);

            // 절대 경로 방식으로 한글 폰트 지정 (resources/NanumGothic.ttf 기준)
            String fontPath = new File(getClass().getResource("/NanumGothic.ttf").toURI()).getAbsolutePath();
            PdfFont font = PdfFontFactory.createFont(fontPath, PdfEncodings.IDENTITY_H); // ✅ 수정됨
            doc.setFont(font);

            doc.add(new Paragraph("📊 광고 리포트"));
            doc.add(new Paragraph("광고 ID: " + report.getAdId()));
            doc.add(new Paragraph("광고명: " + report.getAdName()));
            doc.add(new Paragraph("사용자 ID: " + report.getUserId()));
            doc.add(new Paragraph("광고 목표: " + report.getAdGoal()));
            doc.add(new Paragraph("성과 목표: " + report.getPerformanceGoal()));
            doc.add(new Paragraph(String.format("현재 성과: %.2f%%", report.getResultRate())));
            doc.add(new Paragraph(String.format("교체 후 성과: %.2f%%", report.getPostChangeRate())));
            doc.add(new Paragraph(String.format("성과 향상률: %.2f%%", report.getPostChangeRate() - report.getResultRate())));

            doc.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}