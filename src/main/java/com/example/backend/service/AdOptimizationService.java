package com.example.backend.service;

import com.example.backend.entity.AdInsight;
import com.example.backend.entity.Ad;
import com.example.backend.entity.AdAccount;
import com.example.backend.repository.AdInsightRepository;
import com.example.backend.repository.AdRepository;
import com.example.backend.repository.AdAccountRepository;
// import com.example.backend.repository.AdContentRepository; // 새로운 광고 생성에 필요할 수 있음
// import com.example.backend.service.MetaAdCreatorService; // 새로운 광고 생성에 필요할 수 있음
// import com.example.backend.service.MetaAdUpdater; // 기존 광고 업데이트에 필요할 수 있음

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional; // 트랜잭션 관리
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class AdOptimizationService {

    @Autowired
    private AdInsightRepository adInsightRepository;
    @Autowired
    private AdRepository adRepository;
    @Autowired
    private AdAccountRepository adAccountRepository;
    // @Autowired
    // private MetaAdUpdater metaAdUpdater; // 광고 상태 변경 로직이 필요할 경우 주입
    // @Autowired
    // private MetaAdCreatorService metaAdCreatorService; // 새로운 광고 생성 로직이 필요할 경우 주입

    // 🔴 테스트 및 데모용 하드코딩된 광고 계정 ID.
    private static final String HARDCODED_AD_ACCOUNT_ID_PURE = "266105224922177";

    // 🔴 임시 성능 기준 (CTR 0.5% 미만이면 성과 미달로 간주)
    private static final double MIN_PERFORMANCE_CTR = 0.005; // 0.5%

    /**
     * 광고 계정의 광고들을 확인하여 성과 미달인 광고를 교체(또는 중지)합니다.
     * 이 메서드는 스케줄러를 통해 주기적으로 호출될 예정입니다.
     */
    @Transactional // 광고 상태 변경 등의 DB 작업이 포함되므로 트랜잭션으로 묶습니다.
    public void optimizeAds() {
        System.out.println("✨ [AdOptimization] 광고 최적화 프로세스 시작...");

        Optional<AdAccount> adAccountOpt = adAccountRepository.findByAccountId(HARDCODED_AD_ACCOUNT_ID_PURE);
        if (adAccountOpt.isEmpty()) {
            System.out.println("❌ [AdOptimization] 광고 계정(" + HARDCODED_AD_ACCOUNT_ID_PURE + ")을 찾을 수 없습니다. 최적화 불가.");
            return;
        }
        AdAccount adAccount = adAccountOpt.get();

        // 1. 활성 상태인 광고 목록 가져오기
        // 실제로는 상태 필터를 더 정교하게 적용할 수 있습니다 (예: status="ACTIVE"인 광고만).
        List<Ad> activeAds = adRepository.findByAdAccount(adAccount);

        if (activeAds.isEmpty()) {
            System.out.println("⚠️ [AdOptimization] 최적화할 활성 광고가 없습니다.");
            return;
        }

        // 2. 각 광고의 성과 지표(예: CTR) 확인
        LocalDate today = LocalDate.now();
        // 실제로는 특정 기간(예: 지난 7일)의 데이터를 가져와야 함
        // 지금은 모든 인사이트를 가져와 가장 최신 Insight를 기준으로 CTR을 확인합니다.

        for (Ad ad : activeAds) {
            // 해당 광고의 모든 인사이트 데이터 가져오기
            List<AdInsight> insights = adInsightRepository.findAllByAdId(ad.getAdId()); // AdInsightRepository에
                                                                                        // findAllByAdId(String adId) 필요

            if (insights.isEmpty()) {
                System.out.println("🔎 [AdOptimization] 광고 '" + ad.getName() + "' (ID: " + ad.getAdId()
                        + ")에 대한 성과 데이터가 없습니다. 건너뜜.");
                continue;
            }

            // 가장 최신 CTR 확인 (실제는 기간 내 평균 CTR 등을 계산)
            AdInsight latestInsight = insights.stream()
                    .filter(i -> i.getCtr() != null)
                    .max((i1, i2) -> i1.getDate().compareTo(i2.getDate()))
                    .orElse(null);

            if (latestInsight == null || latestInsight.getCtr().doubleValue() < MIN_PERFORMANCE_CTR) {
                // 3. 성과 미달 광고 처리 (PAUSED 상태로 변경 예시)
                System.out.println("📉 [AdOptimization] 성과 미달 광고 발견: '" + ad.getName() + "' (ID: " + ad.getAdId() + ")"
                        +
                        " - CTR: " + (latestInsight != null ? String.format("%.4f", latestInsight.getCtr()) : "N/A") +
                        ", 기준 미달: " + String.format("%.4f", MIN_PERFORMANCE_CTR));

                // 🔴 여기서 Meta API를 통해 광고 상태를 PAUSED로 변경하는 로직이 필요.
                // (예: metaAdUpdater.updateAdStatus(ad.getAdId(), "PAUSED", accessToken);)
                // 현재는 DB상의 상태만 변경하는 것으로 예시.
                ad.setStatus("PAUSED"); // DB 상태 업데이트
                adRepository.save(ad);
                System.out.println(
                        "✅ [AdOptimization] 광고 '" + ad.getName() + "' (ID: " + ad.getAdId() + ") 상태를 PAUSED로 변경했습니다.");

                // 🔴 여기서 새로운 광고를 활성화하거나 생성하는 로직이 필요.
                // (예: activateNewAd(adAccount, accessToken); 또는 createNewAd(adAccount,
                // accessToken);)
                // 지금은 간단히 로그만 남깁니다.
                System.out.println("💡 [AdOptimization] 새로운 광고 활성화/생성 로직이 여기에 들어갑니다.");

            } else {
                System.out.println("👍 [AdOptimization] 광고 '" + ad.getName() + "' (ID: " + ad.getAdId()
                        + ")는 기준을 충족합니다. CTR: " + String.format("%.4f", latestInsight.getCtr()));
            }
        }
        System.out.println("✨ [AdOptimization] 광고 최적화 프로세스 완료.");
    }

    // 새로운 광고를 활성화하거나 생성하는 헬퍼 메서드는 여기에 구현될 수 있습니다.
    // private void activateNewAd(AdAccount adAccount, String accessToken) { ... }
    // private void createNewAd(AdAccount adAccount, String accessToken) { ... }
}