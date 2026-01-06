package com.example.backend.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

import java.util.UUID;

@Service
public class S3ImageService {

    private final S3Client s3Client;

    @Value("${aws.s3.bucket}")
    private String bucket;

    public S3ImageService(S3Client s3Client) {
        this.s3Client = s3Client;
    }

    public String upload(byte[] imageBytes, String contentType) {
        String key = "ads/" + UUID.randomUUID() + ".jpg";

        PutObjectRequest request = PutObjectRequest.builder()
                .bucket(bucket)
                .key(key)
                .contentType(contentType)
                .build();

        s3Client.putObject(request, RequestBody.fromBytes(imageBytes));

        // ✅ 퍼블릭 URL
        return "https://" + bucket + ".s3.ap-northeast-2.amazonaws.com/" + key;
    }
}
