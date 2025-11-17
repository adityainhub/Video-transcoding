package com.streaming.app.controller;

import com.streaming.app.model.Video;
import com.streaming.app.model.VideoStatus;
import com.streaming.app.service.VideoService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/videos")
@RequiredArgsConstructor
public class VideoRetrievalController {

    private final VideoService videoService;

    // 1. Fetch video by ID
    @GetMapping("/{id}")
    public ResponseEntity<?> getVideoById(@PathVariable Long id) {
        System.out.println("[VideoRetrievalController] GET /api/videos/" + id + " - Fetching video by ID");
        return videoService.getVideoById(id)
                .map(video -> {
                    System.out.println("[VideoRetrievalController] Video found: " + video.getId() + ", status: " + video.getStatus());
                    return ResponseEntity.ok(video);
                })
                .orElseGet(() -> {
                    System.out.println("[VideoRetrievalController] Video not found: " + id);
                    return ResponseEntity.notFound().build();
                });
    }

    // 2. Fetch videos by status (optional)
    @GetMapping("/status/{status}")
    public ResponseEntity<List<Video>> getVideosByStatus(@PathVariable String status) {
        System.out.println("[VideoRetrievalController] GET /api/videos/status/" + status + " - Fetching videos by status");
        VideoStatus videoStatus = VideoStatus.valueOf(status.toUpperCase());
        List<Video> videos = videoService.getVideosByStatus(videoStatus);
        System.out.println("[VideoRetrievalController] Found " + videos.size() + " videos with status: " + videoStatus);
        return ResponseEntity.ok(videos);
    }
}