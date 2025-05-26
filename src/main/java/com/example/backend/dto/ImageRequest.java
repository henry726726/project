package com.example.backend.dto;

public class ImageRequest {
    private String product;
    private String text;

    public String getProduct() {
        return product;
    }
    public void setProduct(String product) {
        this.product = product;
    }

    public String getText() {
        return text;
    }
    public void setText(String text) {
        this.text = text;
    }
}