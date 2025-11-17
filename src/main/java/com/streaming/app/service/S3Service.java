package com.streaming.app.service;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.PresignedPutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.model.PutObjectPresignRequest;

import java.time.Duration;
import java.util.UUID;

@Service
public class S3Service {

    private S3Client s3Client;
    private S3Presigner s3Presigner;

    @Value("${aws.s3.rawBucket}")
    private String rawBucketName;          // renamed ✔️

    @Value("${aws.s3.processedBucket}")
    private String processedBucketName;    // new ✔️

    @Value("${aws.region}")
    private String region;

    @PostConstruct
    public void init() {
        this.s3Client = S3Client.builder()
                .region(Region.of(region))
                .credentialsProvider(DefaultCredentialsProvider.create())
                .build();

        this.s3Presigner = S3Presigner.builder()
                .region(Region.of(region))
                .credentialsProvider(DefaultCredentialsProvider.create())
                .build();
    }

    // Generate presigned URL for RAW uploads
    public String generatePresignedUrl(String s3Key, String contentType) {

        PutObjectRequest putObjectRequest = PutObjectRequest.builder()
                .bucket(rawBucketName)        // uses RAW bucket ✔️
                .key(s3Key)
                .contentType(contentType)
                .build();

        PutObjectPresignRequest presignRequest = PutObjectPresignRequest.builder()
                .signatureDuration(Duration.ofMinutes(30))
                .putObjectRequest(putObjectRequest)
                .build();

        PresignedPutObjectRequest presignedRequest =
                s3Presigner.presignPutObject(presignRequest);

        return presignedRequest.url().toString();
    }

    // Generate RAW bucket key
    public String generateRawVideoKey(String fileName) {
        return "raw-videos/" + UUID.randomUUID() + "-" + fileName;
    }

    // Generate PROCESSED bucket key (for backend usage if needed)
    public String generateProcessedKey(Long videoId, String quality) {
        return "processed-videos/" + videoId + "/" + quality + ".mp4";
    }


    // Delete from bucket
    public void deleteFile(String s3Key) {
        DeleteObjectRequest deleteRequest = DeleteObjectRequest.builder()
                .bucket(processedBucketName)
                .key(s3Key)
                .build();
        s3Client.deleteObject(deleteRequest);
    }

    @PreDestroy
    public void cleanup() {
        if (s3Client != null) s3Client.close();
        if (s3Presigner != null) s3Presigner.close();
    }
}