package com.example.backend.dto;

import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class TextGenerationRequest {
    private String product;
    private String target;
    private String purpose;
    private String keyword;
    private String duration;
}