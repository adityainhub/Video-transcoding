package com.streaming.app.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class VideoDownloadResponse {
    private Long videoId;
    private String status;
    private String message;
    private List<VideoVariantDTO> variants;

}