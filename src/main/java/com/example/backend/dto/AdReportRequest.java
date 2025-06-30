package com.example.backend.dto;

import jakarta.validation.constraints.*;
import lombok.Data;

@Data
public class AdReportRequest {
    @NotBlank
    private String adId;

    @NotBlank
    private String adName;

    @NotBlank
    private String userId;

    private String adGoal;

    private String performanceGoal;

    @PositiveOrZero
    private double resultRate;

    @PositiveOrZero
    private double postChangeRate;
}