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

        System.out.println("[VideoService] saveUploadedVideo - fileName: " + fileName + ", s3Key: " + s3Key);
        Video video = Video.builder()
                .fileName(fileName)
                .s3Key(s3Key)
                .contentType(contentType)
                .status(VideoStatus.UPLOADED)
                .uploadedAt(LocalDateTime.now())
                .build();

        Video saved = videoRepository.save(video);
        System.out.println("[VideoService] Video saved with ID: " + saved.getId());
        return saved;
    }

    public Optional<Video> getVideoById(Long id) {
        System.out.println("[VideoService] getVideoById - id: " + id);
        Optional<Video> video = videoRepository.findById(id);
        System.out.println("[VideoService] Video found: " + video.isPresent());
        return video;
    }

    public List<Video> getVideosByStatus(VideoStatus status) {
        System.out.println("[VideoService] getVideosByStatus - status: " + status);
        List<Video> videos = videoRepository.findByStatus(status);
        System.out.println("[VideoService] Found " + videos.size() + " videos");
        return videos;
    }

    public void markQueued(Long videoId) {
        System.out.println("[VideoService] markQueued - videoId: " + videoId);
        videoRepository.findById(videoId).ifPresent(video -> {
            video.setStatus(VideoStatus.QUEUED);
            videoRepository.save(video);
            System.out.println("[VideoService] Video " + videoId + " status changed to QUEUED");
        });
    }

    public void markAsProcessing(Long videoId) {
        System.out.println("[VideoService] markAsProcessing - videoId: " + videoId);
        videoRepository.findById(videoId).ifPresent(video -> {
            video.setStatus(VideoStatus.PROCESSING);
            videoRepository.save(video);
            System.out.println("[VideoService] Video " + videoId + " status changed to PROCESSING");
        });
    }

    // Save transcoded variants after processing
    public void saveTranscodedVariants(TranscodeResultDTO dto) {

        System.out.println("[VideoService] saveTranscodedVariants - videoId: " + dto.getVideoId() + ", variants: " + dto.getVariants().size());
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
            System.out.println("[VideoService] Video " + video.getId() + " saved with " + variants.size() + " variants, status: PROCESSED");
        });
    }

    // Generate public URL from s3Key
    private String buildPublicUrl(String s3Key) {
        // Ensures base URL ends with /
        String base = publicBaseUrl.endsWith("/") ? publicBaseUrl : publicBaseUrl + "/";
        return base + s3Key;
    }

    public void markAsFailed(Long videoId) {
        System.out.println("[VideoService] markAsFailed - videoId: " + videoId);
        videoRepository.findById(videoId).ifPresent(video -> {
            video.setStatus(VideoStatus.FAILED);
            videoRepository.save(video);
            System.out.println("[VideoService] Video " + videoId + " status changed to FAILED");
        });
    }

    public Long resolveVideoIdFromS3Key(String s3Key) {

        System.out.println("[VideoService] resolveVideoIdFromS3Key - s3Key: " + s3Key);
        // Example: "raw-videos/UUID-test.mp4"
        if (s3Key == null || !s3Key.contains("/")) {
            System.out.println("[VideoService] ERROR: Invalid S3 key format: " + s3Key);
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
            System.out.println("[VideoService] Video found by s3Key: " + byS3Key.get().getId());
            return byS3Key.get().getId();
        }

        // Option 2: lookup by original file name (fallback)
        Optional<Video> byFileName = videoRepository.findByFileName(originalFileName);
        if (byFileName.isPresent()) {
            System.out.println("[VideoService] Video found by fileName: " + byFileName.get().getId());
            return byFileName.get().getId();
        }

        System.out.println("[VideoService] ERROR: No video found for S3 key: " + s3Key);
        throw new IllegalStateException("No video found for S3 key: " + s3Key);
    }
}