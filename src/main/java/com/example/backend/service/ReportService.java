package com.example.backend.service;

import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.URL;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Base64;
import java.util.List;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

import com.example.backend.entity.Ad;
import com.example.backend.entity.AdContent;
import com.example.backend.entity.AdInsight;
import com.example.backend.repository.AdContentRepository;
import com.example.backend.repository.AdInsightRepository;
import com.example.backend.repository.AdRepository;
import com.lowagie.text.Document;
import com.lowagie.text.DocumentException;
import com.lowagie.text.Element;
import com.lowagie.text.Font;
import com.lowagie.text.Image;
import com.lowagie.text.PageSize;
import com.lowagie.text.Paragraph;
import com.lowagie.text.Phrase;
import com.lowagie.text.pdf.BaseFont;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter; // ⭐️ 추가: java.awt.Color 임포트!


@Service
public class ReportService {

    @Autowired
    private AdInsightRepository adInsightRepository;
    @Autowired
    private AdRepository adRepository;
    @Autowired
    // private AdAccountRepository adAccountRepository; // 이 서비스에서는 직접 사용하지 않으나, 빈 주입은 그대로 유지 (주석 처리)
    private AdContentRepository adContentRepository;

    private static final String HARDCODED_AD_ACCOUNT_ID_PURE = "266105224922177"; // 현재 이 서비스에서 직접 사용하지 않음
    private static final String RESOURCE_PATH_NANUM_GOTHIC = "/fonts/NanumGothic.ttf";

    // ⭐️ 블랙맘바! 여기가 네 아이디어를 구현하는 핵심 부분이야!
    // PDF 리포트 생성 시 항상 고정된 ad_id와 날짜의 데이터를 사용하도록 임시로 고정
    private static final String FIXED_REPORT_AD_ID = "120229614252300615"; // 고정된 광고 ID
    private static final LocalDate FIXED_REPORT_DATE = LocalDate.of(2025, 9, 6); // 고정된 날짜


    public byte[] generateAdPerformancePdfBytes() throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        Document document = new Document(PageSize.A4);

        BaseFont baseFont = null;
        try {
            // ClassPathResource를 사용할 때 getURL().getPath()가 한글이나 특수문자 경로에서 문제 발생할 수 있음
            // getPath() 대신 getFile().getAbsolutePath() 또는 Stream으로 직접 읽는 방식이 더 안전함
            baseFont = BaseFont.createFont(new ClassPathResource(RESOURCE_PATH_NANUM_GOTHIC).getPath(), BaseFont.IDENTITY_H, BaseFont.EMBEDDED);
        } catch (DocumentException | IOException e) {
            System.err.println("❌ 한글 폰트 로드 실패: " + e.getMessage() + ". 기본 폰트 사용.");
            // 폰트 로드 실패 시 에러만 출력하고 기본 폰트(HELVETICA)로 대체하여 PDF 생성 프로세스 계속 진행
            baseFont = BaseFont.createFont(BaseFont.HELVETICA, BaseFont.WINANSI, BaseFont.NOT_EMBEDDED);
        }

        Font titleFont = new Font(baseFont, 18, Font.BOLD);
        Font sectionFont = new Font(baseFont, 14, Font.BOLD);
        Font contentFont = new Font(baseFont, 10, Font.NORMAL);
        Font tableHeaderFont = new Font(baseFont, 10, Font.BOLD);
        Font tableCellFont = new Font(baseFont, 8, Font.NORMAL);


        try {
            PdfWriter.getInstance(document, baos);
            document.open();

            // 리포트 기본 정보 추가
            document.add(new Paragraph("광고 성과 리포트", titleFont));
            document.add(new Paragraph("생성일: " + LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd")), contentFont));
            document.add(new Paragraph("\n"));


            // ⭐️ 여기부터 데이터 조회 로직을 수정합니다.
            // 이제 특정 adId에 해당하는 Ad를 하나만 가져옵니다.
            Optional<Ad> fixedAdOpt = adRepository.findByAdId(FIXED_REPORT_AD_ID); // AdRepository에 findByAdId 메서드가 필요합니다.
            if (fixedAdOpt.isEmpty()) {
                document.add(new Paragraph("고정된 광고 ID " + FIXED_REPORT_AD_ID + "을(를) 찾을 수 없습니다.", contentFont));
                document.close();
                throw new RuntimeException("Fixed Ad not found for PDF report generation.");
            }
            Ad fixedAd = fixedAdOpt.get(); // 찾은 광고 엔티티

            // 특정 adId와 FIXED_REPORT_DATE에 해당하는 모든 AdInsight 데이터를 가져옵니다.
            List<AdInsight> insightsForFixedAd = adInsightRepository.findAllByAdIdAndDate(fixedAd.getAdId(), FIXED_REPORT_DATE);

            if (insightsForFixedAd.isEmpty()) {
                document.add(new Paragraph("고정된 광고(" + FIXED_REPORT_AD_ID + ")에 대한 " + FIXED_REPORT_DATE + " 날짜의 성과 데이터를 찾을 수 없습니다.", contentFont));
                document.close();
                throw new RuntimeException("No insights found for fixed ad and date for PDF report generation.");
            }

            // 고정된 광고 하나에 대한 정보만 리포트에 추가
            document.add(new Paragraph("---", contentFont));
            document.add(new Paragraph("광고: " + fixedAd.getName() + " (ID: " + fixedAd.getAdId() + ")", sectionFont));
            document.add(new Paragraph("상태: " + (fixedAd.getStatus() != null ? fixedAd.getStatus() : "알 수 없음"), contentFont));
            document.add(new Paragraph("\n"));

            // AdContent 정보 조회 및 추가
            Optional<AdContent> adContentOpt = adContentRepository.findByAdId(fixedAd.getAdId());
            if (adContentOpt.isPresent()) {
                AdContent adContent = adContentOpt.get();
                if (adContent.getAdText() != null && !adContent.getAdText().isEmpty()) {
                    document.add(new Paragraph("광고 문구:", sectionFont));
                    document.add(new Paragraph(adContent.getAdText(), contentFont));
                    document.add(new Paragraph("\n"));
                }

                if (adContent.getImageUrl() != null && !adContent.getImageUrl().isEmpty()) {
                    try {
                        Image img = null;
                        String imageUrl = adContent.getImageUrl().trim();

                        // Base64 인코딩된 이미지인지 URL 이미지인지 판단
                        boolean isBase64Encoded = imageUrl.startsWith("data:image/") || (imageUrl.length() > 200 && !imageUrl.contains("://"));

                        if (isBase64Encoded) {
                            String base64Image = imageUrl;
                            if (base64Image.indexOf(",") > 0) { // "data:image/png;base64," 같은 프리픽스 제거
                                base64Image = base64Image.substring(base64Image.indexOf(",") + 1);
                            }
                            byte[] imageBytes = Base64.getDecoder().decode(base64Image);
                            img = Image.getInstance(imageBytes);
                        } else {
                            img = Image.getInstance(new URL(imageUrl));
                        }

                        if (img != null) {
                            img.scaleToFit(500, 500); // 이미지 크기 조절
                            img.setAlignment(Element.ALIGN_CENTER);
                            document.add(new Paragraph("광고 이미지:", sectionFont));
                            document.add(img);
                            document.add(new Paragraph("\n"));
                        }
                    } catch (Exception e) {
                        System.err.println("❌ 광고 이미지 로드 또는 디코딩 실패 (데이터 시작: " + adContent.getImageUrl().substring(0, Math.min(adContent.getImageUrl().length(), 100)) + "...): " + e.getMessage());
                        document.add(new Paragraph("광고 이미지를 로드/디코딩할 수 없습니다.", contentFont));
                        document.add(new Paragraph("\n"));
                    }
                }
            } else {
                document.add(new Paragraph("광고 문구 및 이미지 데이터를 찾을 수 없습니다.", contentFont));
                document.add(new Paragraph("\n"));
            }

            // insightsForFixedAd (고정된 광고의 인사이트)에서 최신 데이터를 찾음
            // (이미 특정 날짜의 데이터만 가져왔으므로, 리스트 내에서는 단일 광고/날짜에 대한 여러 연령/성별 Breakdown 데이터가 있을 수 있음)
            // 여기서는 통계적으로 유의미한 대표 Insight 하나를 찾거나, 그냥 리스트의 첫 번째 항목을 사용
            // 여기서는 get()을 통해 리스트의 첫 번째 요소를 사용하도록 변경 (더 단순)
            AdInsight representativeInsight = insightsForFixedAd.isEmpty() ? null : insightsForFixedAd.get(0);


            if (representativeInsight != null) {
                // 주요 지표 요약 섹션 (대표 인사이트 기반)
                document.add(new Paragraph("주요 지표 요약:", sectionFont));
                PdfPTable summaryTable = new PdfPTable(4);
                summaryTable.setWidthPercentage(100);
                summaryTable.setSpacingBefore(10f);
                summaryTable.setSpacingAfter(10f);

                addTableCell(summaryTable, "노출수", tableHeaderFont, true);
                addTableCell(summaryTable, "클릭수", tableHeaderFont, true);
                addTableCell(summaryTable, "광고비", tableHeaderFont, true);
                addTableCell(summaryTable, "CTR", tableHeaderFont, true);

                addTableCell(summaryTable, String.valueOf(representativeInsight.getImpressions()), tableCellFont, false);
                addTableCell(summaryTable, String.valueOf(representativeInsight.getClicks()), tableCellFont, false);
                addTableCell(summaryTable, String.format("₩%.2f", representativeInsight.getSpend()), tableCellFont, false);
                addTableCell(summaryTable, String.format("%.2f%%", representativeInsight.getCtr()), tableCellFont, false);
                document.add(summaryTable);
                document.add(new Paragraph("\n"));


                // 연령 및 성별 분석 테이블 (insightsForFixedAd의 모든 breakdown 데이터를 사용)
                document.add(new Paragraph("연령 및 성별 분석:", sectionFont));
                PdfPTable breakdownTable = new PdfPTable(5);
                breakdownTable.setWidthPercentage(100);
                breakdownTable.setSpacingBefore(10f);
                breakdownTable.setSpacingAfter(10f);

                addTableCell(breakdownTable, "연령", tableHeaderFont, true);
                addTableCell(breakdownTable, "성별", tableHeaderFont, true);
                addTableCell(breakdownTable, "노출수", tableHeaderFont, true);
                addTableCell(breakdownTable, "클릭수", tableHeaderFont, true);
                addTableCell(breakdownTable, "광고비", tableHeaderFont, true);

                // ⭐️ 이제 insightsForFixedAd의 데이터를 활용하여 연령/성별 분석 테이블 채우기
                // 이 리스트는 이제 특정 광고와 날짜에 대한 '모든' 연령/성별 Breakdown 데이터를 포함합니다.
                for (AdInsight insight : insightsForFixedAd) {
                    addTableCell(breakdownTable, insight.getAge() != null ? insight.getAge() : "알 수 없음", tableCellFont, false);
                    addTableCell(breakdownTable, insight.getGender() != null ? (insight.getGender().equals("female") ? "여성" : (insight.getGender().equals("male") ? "남성" : "알 수 없음")) : "알 수 없음", tableCellFont, false);
                    addTableCell(breakdownTable, String.valueOf(insight.getImpressions()), tableCellFont, false);
                    addTableCell(breakdownTable, String.valueOf(insight.getClicks()), tableCellFont, false);
                    addTableCell(breakdownTable, String.format("₩%.2f", insight.getSpend()), tableCellFont, false);
                }
                document.add(breakdownTable);
                document.add(new Paragraph("\n\n")); // 기존 Paragraph("\n") 대신 "\n\n"으로 여백을 더 줍니다.

            } else {
                 document.add(new Paragraph("이 고정된 광고에 대한 성과 데이터를 찾을 수 없습니다.", contentFont));
                 document.add(new Paragraph("\n\n")); // 기존 Paragraph("\n") 대신 "\n\n"으로 여백을 더 줍니다.
            }

            document.close();
            return baos.toByteArray();

        } catch (DocumentException e) {
            System.err.println("❌ PDF 생성 중 오류 발생: " + e.getMessage());
            throw new IOException("Failed to generate PDF report", e);
        }
    }

    // PDF 테이블 셀을 추가하는 헬퍼 메서드
    private void addTableCell(PdfPTable table, String text, Font font, boolean isHeader) {
        PdfPCell cell = new PdfPCell(new Phrase(text, font));
        cell.setHorizontalAlignment(Element.ALIGN_CENTER);
        cell.setVerticalAlignment(Element.ALIGN_MIDDLE);
        cell.setPadding(5);
        if (isHeader) {
            cell.setBackgroundColor(new Color(230, 230, 230)); // ⭐️ 수정: java.awt.Color 사용
        }
        table.addCell(cell);
    }
}