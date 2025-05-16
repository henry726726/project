package com.example.backend.dto;

import lombok.Data;

@Data
public class ContentRequest {
    private String userdatainputId;
    private String caption;
    private String imageUrl;
}
