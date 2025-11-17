package com.streaming.app.model;

import jakarta.persistence.Embeddable;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Embeddable
@Data
@NoArgsConstructor
@AllArgsConstructor
public class VideoVariant {
    private String quality;
    private String s3Key;
    private String url;
    private String contentType;
}