package com.example.demo.product;

import jakarta.validation.constraints.*;

public class ProductRequest {

    @NotBlank(message = "상품명은 필수입니다.")
    private String name;

    @NotBlank(message = "상품 설명은 필수입니다.")
    private String description;

    // Getter / Setter
}
