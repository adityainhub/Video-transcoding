package com.streaming.app.controller;

import com.streaming.app.dto.TranscodeResultDTO;
import com.streaming.app.model.Video;
import com.streaming.app.model.VideoStatus;
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

        System.out.println("[VideoCallbackController] POST /api/videos/" + id + "/completed - Callback received");
        System.out.println("[VideoCallbackController] Results videoId: " + results.getVideoId() + ", variants count: " + results.getVariants().size());
        if (!id.equals(results.getVideoId())) {
            System.out.println("[VideoCallbackController] ERROR: Video ID mismatch - path: " + id + ", body: " + results.getVideoId());
            return ResponseEntity.badRequest().body(ResponseMessages.VIDEO_ID_MISMATCH);
        }

        videoService.saveTranscodedVariants(results);
        System.out.println("[VideoCallbackController] Video " + id + " marked as PROCESSED");
        return ResponseEntity.ok(String.format(ResponseMessages.VIDEO_PROCESSED_FORMAT, id));
    }

    // Mark video as PROCESSING
    @PostMapping("/{videoId}/processing")
    public ResponseEntity<Void> markProcessing(@PathVariable Long videoId) {
        System.out.println("[VideoController] POST /api/videos/" + videoId + "/processing - Marking video as processing");

        videoService.markAsProcessing(videoId);

        System.out.println("[VideoController] Video " + videoId + " status updated to PROCESSING");
        return ResponseEntity.ok().build();
    }

    @PostMapping("/{id}/failed")
    public ResponseEntity<String> handleVideoFailed(@PathVariable Long id){
        System.out.println("[VideoCallbackController] POST /api/videos/" + id + "/failed - Failure callback received");
        videoService.markAsFailed(id);
        System.out.println("[VideoCallbackController] Video " + id + " marked as FAILED");
        return ResponseEntity.ok(String.format(ResponseMessages.VIDEO_FAILED_FORMAT, id));
    }
}
