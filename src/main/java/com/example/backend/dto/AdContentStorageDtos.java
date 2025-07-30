package com.example.backend.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

// 💡 광고 콘텐츠 저장 및 전송 관련 DTO들을 한 파일에 모았습니다.
// 각 DTO는 AdContentStorageDtos 클래스 내부에 static public 클래스로 정의되어 독립적으로 사용될 수 있습니다.
// 이 DTO들은 AdGenerationParameters를 상속받아 공통 필드의 중복을 제거했습니다.
public class AdContentStorageDtos {

    // =======================================================
    // ContentRequest.java의 통합 내용
    // (기존의 ContentRequest는 삭제하고 이 내부 클래스를 사용합니다)
    // =======================================================
    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor // Lombok을 통해 AllArgsConstructor 추가 (AdGenerationParameters의 필드까지 포함)
    public static class ContentRequest extends AdGenerationParameters { // AdGenerationParameters를 상속받습니다.

        private String userId; // UserDataInputController 고유 필드
        private String name;   // UserDataInputController 고유 필드

        private String caption; // 선택된 문구
        private String imageUrl; // 이미지 URL

        /*
         * Lombok의 @NoArgsConstructor와 @AllArgsConstructor가 자동으로 생성자를 만들어주므로,
         * 아래 수동 생성자 코드는 일반적으로 필요 없습니다.
         *
         * // 예시: 모든 필드를 포함하는 수동 생성자 (Lombok 사용 시 생략 가능)
         * public ContentRequest(String product, String target, String purpose, String keyword, String duration,
         *                       String userId, String name, String caption, String imageUrl) {
         *     super(product, target, purpose, keyword, duration); // 부모 클래스 생성자 호출
         *     this.userId = userId;
         *     this.name = name;
         *     this.caption = caption;
         *     this.imageUrl = imageUrl;
         * }
         */
    }

    // =======================================================
    // SaveAdContentRequest.java의 통합 내용
    // (기존의 SaveAdContentRequest는 삭제하고 이 내부 클래스를 사용합니다)
    // =======================================================
    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor // Lombok을 통해 AllArgsConstructor 추가 (AdGenerationParameters의 필드까지 포함)
    public static class SaveAdContentRequest extends AdGenerationParameters { // AdGenerationParameters를 상속받습니다.

        // 생성된 광고 문구 (SaveAdContentRequest 고유 필드)
        private String adText;

        // 합성된 이미지 (Base64 인코딩된 문자열) (SaveAdContentRequest 고유 필드)
        private String generatedImageBase64;

        /*
         * Lombok의 @NoArgsConstructor와 @AllArgsConstructor가 자동으로 생성자를 만들어주므로,
         * 아래 수동 생성자 코드는 일반적으로 필요 없습니다.
         *
         * // 예시: 모든 필드를 포함하는 수동 생성자 (Lombok 사용 시 생략 가능)
         * public SaveAdContentRequest(String product, String target, String purpose, String keyword, String duration,
         *                             String adText, String generatedImageBase64) {
         *     super(product, target, purpose, keyword, duration); // 부모 클래스 생성자 호출
         *     this.adText = adText;
         *     this.generatedImageBase64 = generatedImageBase64;
         * }
         */
    }
}