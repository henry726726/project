package com.example.backend.dto;

import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class CreateAdRequest {
    private String billingEvent;
    private String optimizationGoal;
    private String bidStrategy;
    private Long dailyBudget; // Meta API는 예산을 센트(또는 최소 통화 단위)로 받으므로 Long 타입이 적절
    private String startTime; // ISO 8601 형식의 문자열 (예: "2025-07-01T10:00:00")
    // 추가로 광고 문구나 이미지 ID, 광고 계정 ID 등이 필요할 수 있습니다.
    // 현재는 FacebookInput.jsx에서 보내는 정보만 포함합니다.
}