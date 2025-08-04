package com.example.backend.dto;

// import lombok.AllArgsConstructor; // 💡💡💡 이 임포트 라인을 삭제합니다! 💡💡💡
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor // 기본 생성자
// @AllArgsConstructor // 💡💡💡 이 어노테이션을 삭제합니다! 💡💡💡
public class TextGenerationResponse {
    private List<String> adTexts; // 광고 문구 리스트

    // 💡💡💡 List<String> adTexts를 인자로 받는 생성자는 그대로 유지합니다. 💡💡💡
    public TextGenerationResponse(List<String> adTexts) {
        this.adTexts = adTexts;
    }
}