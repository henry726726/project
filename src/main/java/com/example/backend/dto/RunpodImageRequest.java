package com.example.backend.dto;

import lombok.Getter;
import lombok.Setter;
import org.springframework.web.multipart.MultipartFile;

@Getter
@Setter
public class RunpodImageRequest {
    private MultipartFile image;
    private String caption;
    private String product;
}
