package com.streaming.app.service;

import com.streaming.app.model.Video;
import com.streaming.app.model.VideoStatus;
import com.streaming.app.repository.VideoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class VideoService {

    private final VideoRepository videoRepository;

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

    public Optional<Video> getVideoById(Long id)
    {
        return videoRepository.findById(id);
    }

    // Get videos by status (e.g., PROCESSING queue)
    public List<Video> getVideosByStatus(VideoStatus status) {
        return videoRepository.findByStatus(status);
    }

    public void markQueued(Long videoId) {
        videoRepository.findById(videoId).ifPresent(video -> {
            video.setStatus(VideoStatus.QUEUED);
            videoRepository.save(video);
        });
    }

    // Mark video as PROCESSING
    public void markAsProcessing(Long videoId) {
        videoRepository.findById(videoId).ifPresent(video -> {
            video.setStatus(VideoStatus.PROCESSING);
            videoRepository.save(video);
        });
    }

    // Mark video as PROCESSED
    public void markAsProcessed(Long videoId) {
        videoRepository.findById(videoId).ifPresent(video -> {
            video.setStatus(VideoStatus.PROCESSED);
            video.setProcessedAt(LocalDateTime.now());
            videoRepository.save(video);
        });
    }

    // Mark video as FAILED
    public void markAsFailed(Long videoId) {
        videoRepository.findById(videoId).ifPresent(video -> {
            video.setStatus(VideoStatus.FAILED);
            videoRepository.save(video);
        });
    }

}