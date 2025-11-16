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
        return videoService.getVideoById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }


    // 3. Fetch videos by status (optional)
    @GetMapping("/status/{status}")
    public ResponseEntity<List<Video>> getVideosByStatus(@PathVariable String status) {
        VideoStatus videoStatus = VideoStatus.valueOf(status.toUpperCase());
        return ResponseEntity.ok(videoService.getVideosByStatus(videoStatus));
    }
}