package com.example.backend.service;

import java.util.List;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import com.example.backend.entity.Ad;
import com.example.backend.entity.AdAccount;
import com.example.backend.repository.AccessTokenRepository;
import com.example.backend.repository.AdAccountRepository;
import com.example.backend.repository.AdRepository;

@Service
public class SchedulerService {

    @Autowired
    private com.example.backend.service.AdInsightService adInsightService;

    @Autowired
    private com.example.backend.service.AdSyncService adSyncService;

    @Autowired
    private AdRepository adRepository;

    @Autowired
    private AdAccountRepository adAccountRepository;

    @Autowired
    private AccessTokenRepository accessTokenRepository;

    @Autowired
    private com.example.backend.service.ReportService reportService;

    @Autowired
    private com.example.backend.service.AdOptimizationService adOptimizationService;

    private static final String HARDCODED_ACCESS_TOKEN = "EAAKBR2AlzfIBPFLuJWZAVF084H8vZAEREdwePHgcBPQd4DadDNIDy81JQha3np6NLSv15GWdHfXWY6DMWTxteS1GFahzMmq94KFfuOUhsJoGkBsAwlzKRTtXZBZB9e57eqMpnXIoYVkAZANztQVF5OtBCvsALFmqGKJFxZCNgwoqNeuMRqXMOLLRYi3f3cOeEy";
    private static final String HARDCODED_AD_ACCOUNT_ID_PURE = "266105224922177";
    private static final String HARDCODED_AD_ACCOUNT_ID_FULL = "act_266105224922177";

    private static final String REPORT_OUTPUT_DIR = "reports/";

    @Scheduled(fixedRate = 600000) // 10분마다 광고 성과 데이터 수집 (운영 시 조절 가능)
    public void scheduleFetchAdInsights() {
        System.out.println("⏰ [Scheduler] 광고 성과 데이터 수집 스케줄러 실행 중...");
        try {
            String accessToken = HARDCODED_ACCESS_TOKEN;
            String adAccountId = HARDCODED_AD_ACCOUNT_ID_PURE;

            Optional<AdAccount> adAccountOpt = adAccountRepository.findByAccountId(adAccountId);
            if (adAccountOpt.isPresent()) {
                AdAccount adAccount = adAccountOpt.get();
                List<Ad> ads = adRepository.findByAdAccount(adAccount);
                if (ads.isEmpty()) {
                    System.out.println("⚠️ [Scheduler] 해당 광고 계정(" + adAccountId + ")에 광고가 없습니다.");
                    return;
                }
                for (Ad ad : ads) {
                    System.out.println("📈 [Scheduler] '" + ad.getName() + "' (ID:" + ad.getAdId() + ") 성과 수집 시작.");
                    adInsightService.fetchAndStoreInsights(ad.getAdId(), accessToken);
                }
            } else {
                System.out.println("❌ [Scheduler] 광고 계정(" + adAccountId + ")을 DB에서 찾을 수 없습니다.");
            }
            System.out.println("✅ [Scheduler] 광고 성과 데이터 수집 완료!");
        } catch (Exception e) {
            System.err.println("❌ 광고 성과 데이터 수집 중 오류: " + e.getMessage());
            e.printStackTrace();
        }
    }

    @Scheduled(fixedRate = 1200000) // 20분마다 광고 상태 동기화
    public void scheduleSyncAdStatuses() {
        System.out.println("⏰ [Scheduler] 광고 상태 동기화 스케줄러 실행 중...");
        try {
            String accessToken = HARDCODED_ACCESS_TOKEN;
            String adAccountIdFull = HARDCODED_AD_ACCOUNT_ID_FULL;
            adSyncService.syncAdsFromMeta(adAccountIdFull, accessToken);
            System.out.println("✅ [Scheduler] 광고 상태 동기화 완료!");
        } catch (Exception e) {
            System.err.println("❌ 광고 상태 동기화 중 오류: " + e.getMessage());
            e.printStackTrace();
        }
    }

    // 📌 PDF 생성 스케줄러는 지금 이렇게 주석 처리하거나 완전히 삭제하세요!
    /*
    @Scheduled(fixedRate = 600000)
    public void scheduleGeneratePdfReportAndSaveLocally() {
        System.out.println("⏰ [Scheduler] PDF 리포트 생성 및 저장 중...");
        try {
            byte[] pdfBytes = reportService.generateAdPerformancePdfBytes();
            Files.createDirectories(Paths.get(REPORT_OUTPUT_DIR));
            String filename = "Ad_Performance_Report_" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss")) + ".pdf";
            String fullPath = REPORT_OUTPUT_DIR + filename;
            try (FileOutputStream fos = new FileOutputStream(fullPath)) {
                fos.write(pdfBytes);
                System.out.println("✅ PDF 저장 완료: " + fullPath);
            }
        } catch (IOException e) {
            System.err.println("❌ PDF 생성/저장 중 오류: " + e.getMessage());
            e.printStackTrace();
        }
    }
    */

    @Scheduled(fixedRate = 2400000) // 40분마다 광고 최적화
    public void scheduleAdOptimization() {
        System.out.println("⏰ [Scheduler] 광고 최적화 스케줄러 실행 중...");
        try {
            adOptimizationService.optimizeAds();
            System.out.println("✅ [Scheduler] 광고 최적화 완료!");
        } catch (Exception e) {
            System.err.println("❌ 광고 최적화 중 오류: " + e.getMessage());
            e.printStackTrace();
        }
    }
}