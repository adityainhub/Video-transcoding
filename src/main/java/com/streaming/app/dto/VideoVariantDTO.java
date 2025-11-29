package com.streaming.app.dto;

import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class VideoVariantDTO {
    private String quality;
    private String url;
    private String contentType;

}