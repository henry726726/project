// src/main/java/com/example/backend/dto/AdAccountDto.java
package com.example.backend.dto;

import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class AdAccountDto {
    private String accountId;
    private String accessToken; // 메타 액세스 토큰. OAuth2 토큰과는 다름.
}