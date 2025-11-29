package com.streaming.app.controller;

import com.streaming.app.dto.VideoDownloadResponse;
import com.streaming.app.dto.VideoVariantDTO;
import com.streaming.app.model.Video;
import com.streaming.app.model.VideoStatus;
import com.streaming.app.service.S3Service;
import com.streaming.app.service.VideoService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/video")
public class VideoDownloadController {


    private VideoService videoService;
    private S3Service s3Service;

    public VideoDownloadController(VideoService videoService, S3Service s3Service) {
        this.videoService = videoService;
        this.s3Service = s3Service;
    }

    @GetMapping("/{id}/download")
    public ResponseEntity<VideoDownloadResponse> getVideoDownloadLinks(@PathVariable("id") Long id) {
        System.out.println("[VideoDownloadController] GET /api/video/" + id + "/download");

        Optional<Video> maybeVideo = videoService.getVideoById(id);
        if (maybeVideo.isEmpty()) {
            System.out.println("[VideoDownloadController] Video not found: " + id);
            return ResponseEntity.notFound().build();
        }

        Video video = maybeVideo.get();

        // Check if video is still processing
        if (video.getStatus() != VideoStatus.PROCESSED) {
            System.out.println("[VideoDownloadController] Video still processing: " + id +
                    ", status: " + video.getStatus());
            return ResponseEntity.status(HttpStatus.ACCEPTED)
                    .body(new VideoDownloadResponse(
                            video.getId(),
                            video.getStatus().toString(),
                            "Video is still being processed",
                            null
                    ));
        }

        // Check if variants exist
        if (video.getVariants() == null || video.getVariants().isEmpty()) {
            System.out.println("[VideoDownloadController] No variants available for video: " + id);
            return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
        }

        // Generate presigned URLs for each variant (expires in 1 hour)
        List<VideoVariantDTO> variantDTOs = video.getVariants().stream()
                .map(variant -> {
                    // Generate temporary presigned download URL
                    String presignedUrl = s3Service.generatePresignedDownloadUrl(
                            variant.getS3Key(),
                            3600  // 1 hour = 3600 seconds
                    );

                    return new VideoVariantDTO(
                            variant.getQuality(),
                            presignedUrl,
                            variant.getContentType()
                    );
                })
                .collect(Collectors.toList());

        System.out.println("[VideoDownloadController] Returning " + variantDTOs.size() +
                " variants for video: " + id);

        return ResponseEntity.ok(new VideoDownloadResponse(
                video.getId(),
                video.getStatus().toString(),
                "Video ready for download. URLs expire in 1 hour.",
                variantDTOs
        ));
    }
}