package com.streaming.app.service;

import com.streaming.app.dto.TranscodeResultDTO;
import com.streaming.app.model.Video;
import com.streaming.app.model.VideoStatus;
import com.streaming.app.model.VideoVariant;
import com.streaming.app.repository.VideoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class VideoService {

    private final VideoRepository videoRepository;

    @Value("${aws.s3.publicBaseUrl}")
    private String publicBaseUrl;

    // Save metadata when upload URL is generated
    public Video saveUploadedVideo(String fileName, String s3Key, String contentType) {

        Video video = Video.builder()
                .fileName(fileName)
                .s3Key(s3Key)
                .contentType(contentType)
                .status(VideoStatus.UPLOADED)
                .uploadedAt(LocalDateTime.now())
                .build();

        return videoRepository.save(video);
    }

    public Optional<Video> getVideoById(Long id) {
        return videoRepository.findById(id);
    }

    public List<Video> getVideosByStatus(VideoStatus status) {
        return videoRepository.findByStatus(status);
    }

    public void markQueued(Long videoId) {
        videoRepository.findById(videoId).ifPresent(video -> {
            video.setStatus(VideoStatus.QUEUED);
            videoRepository.save(video);
        });
    }

    public void markAsProcessing(Long videoId) {
        videoRepository.findById(videoId).ifPresent(video -> {
            video.setStatus(VideoStatus.PROCESSING);
            videoRepository.save(video);
        });
    }

    // Save transcoded variants after processing
    public void saveTranscodedVariants(TranscodeResultDTO dto) {

        videoRepository.findById(dto.getVideoId()).ifPresent(video -> {

            List<VideoVariant> variants = dto.getVariants().stream()
                    .map(v -> new VideoVariant(
                            v.getQuality(),
                            v.getS3Key(),
                            buildPublicUrl(v.getS3Key()),
                            v.getContentType() != null ? v.getContentType() : video.getContentType()
                    ))
                    .toList();

            video.setVariants(variants);
            video.setStatus(VideoStatus.PROCESSED);
            video.setProcessedAt(LocalDateTime.now());

            videoRepository.save(video);
        });
    }

    // Generate public URL from s3Key
    private String buildPublicUrl(String s3Key) {
        // Ensures base URL ends with /
        String base = publicBaseUrl.endsWith("/") ? publicBaseUrl : publicBaseUrl + "/";
        return base + s3Key;
    }

    public void markAsFailed(Long videoId) {
        videoRepository.findById(videoId).ifPresent(video -> {
            video.setStatus(VideoStatus.FAILED);
            videoRepository.save(video);
        });
    }

    public Long resolveVideoIdFromS3Key(String s3Key) {

        // Example: "raw-videos/UUID-test.mp4"
        if (s3Key == null || !s3Key.contains("/")) {
            throw new IllegalArgumentException("Invalid S3 key format: " + s3Key);
        }

        // Extract "UUID-test.mp4"
        String fileNamePart = s3Key.substring(s3Key.lastIndexOf("/") + 1);

        // Extract original filename ("test.mp4")
        int dashIndex = fileNamePart.indexOf("-");
        if (dashIndex == -1) {
            throw new IllegalArgumentException("S3 key does not contain expected UUID prefix: " + s3Key);
        }

        String originalFileName = fileNamePart.substring(dashIndex + 1);

        // Option 1: direct lookup by exact s3Key
        Optional<Video> byS3Key = videoRepository.findByS3Key(s3Key);
        if (byS3Key.isPresent()) {
            return byS3Key.get().getId();
        }

        // Option 2: lookup by original file name (fallback)
        Optional<Video> byFileName = videoRepository.findByFileName(originalFileName);
        if (byFileName.isPresent()) {
            return byFileName.get().getId();
        }

        throw new IllegalStateException("No video found for S3 key: " + s3Key);
    }
}