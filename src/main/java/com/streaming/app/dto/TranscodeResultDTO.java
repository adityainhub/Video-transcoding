package com.streaming.app.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class TranscodeResultDTO {

    @NotNull(message = "videoId is required")
    private Long videoId;

    @NotNull(message = "variants list is required")
    @Valid
    @Size(min = 1, message = "At least one variant is required")
    private List<VariantDTO> variants;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class VariantDTO {

        @NotBlank
        private String quality;      // e.g., "1080p"

        @NotBlank
        private String s3Key;        // e.g., "processed-videos/123/1080p.mp4"

        @NotBlank
        private String url;         // Complete S3 public URL

        private String contentType;  // video/mp4
    }
}