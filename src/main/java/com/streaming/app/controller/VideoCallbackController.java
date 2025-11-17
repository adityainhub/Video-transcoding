package com.streaming.app.controller;

import com.streaming.app.dto.TranscodeResultDTO;
import com.streaming.app.service.VideoService;
import com.streaming.app.util.ResponseMessages;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/videos")
@RequiredArgsConstructor
public class VideoCallbackController {

    private final VideoService videoService;

    @PostMapping("/{id}/completed")
    public ResponseEntity<String> handleVideoCompleted(
            @PathVariable Long id,
            @Valid @RequestBody TranscodeResultDTO results) {

        if (!id.equals(results.getVideoId())) {
            return ResponseEntity.badRequest().body(ResponseMessages.VIDEO_ID_MISMATCH);
        }

        videoService.saveTranscodedVariants(results);
        return ResponseEntity.ok(String.format(ResponseMessages.VIDEO_PROCESSED_FORMAT, id));
    }

    @PostMapping("/{id}/failed")
    public ResponseEntity<String> handleVideoFailed(@PathVariable Long id){
        videoService.markAsFailed(id);
        return ResponseEntity.ok(String.format(ResponseMessages.VIDEO_FAILED_FORMAT, id));
    }
}
