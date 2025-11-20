package com.streaming.app.controller;

import com.streaming.app.service.SqsMessageProducer;
import com.streaming.app.model.Video;
import com.streaming.app.model.VideoStatus;
import com.streaming.app.service.S3Service;
import com.streaming.app.service.VideoService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("api/video")
public class videoUploadController {

    @Autowired
    private S3Service s3Service;
    @Autowired
    private VideoService videoService;

    @Autowired
    private SqsMessageProducer sqsMessageProducer;


    // Endpoint to get presigned upload URL
    @PostMapping("/upload-url")
    public ResponseEntity<Map<String,String>> getUploadUrl(@RequestBody Map<String,String> request)
    {
        System.out.println("[videoUploadController] POST /upload-url - Request received");
        String fileName= request.get("fileName");
        String contentType= request.get("contentType");
        System.out.println("[videoUploadController] fileName: " + fileName + ", contentType: " + contentType);

        String s3Key = s3Service.generateRawVideoKey(fileName);
        String presignedUrl = s3Service.generatePresignedUrl(s3Key, contentType);

        Video video = videoService.saveUploadedVideo(fileName, s3Key, contentType);
        System.out.println("[videoUploadController] Video saved with ID: " + video.getId());

        Map<String,String> response = Map.of(
                "presignedUrl",presignedUrl,
                "s3key",s3Key,
                "videoId", video.getId().toString()
        );
        System.out.println("[videoUploadController] Returning presigned URL and videoId: " + video.getId());
        return ResponseEntity.ok(response);
    }

    // Endpoint to handle upload completion notification
   @PostMapping("/videos/uploaded")
    public ResponseEntity<String> onUploadComplete(
            @RequestBody Map<String, Object> event) {

        System.out.println("[videoUploadController] POST /videos/uploaded - Upload completion event received");
        
        try {
            Map<String, Object> detail = (Map<String, Object>) event.get("detail");
            Map<String, Object> bucket = (Map<String, Object>) detail.get("bucket");
            Map<String, Object> object = (Map<String, Object>) detail.get("object");

            String bucketName = bucket.get("name").toString();
            String s3Key = object.get("key").toString();
            System.out.println("[videoUploadController] bucketName: " + bucketName + ", s3Key: " + s3Key);

            // Extract videoId from s3Key using your naming convention
            Long videoId;
            try {
                videoId = videoService.resolveVideoIdFromS3Key(s3Key);
                System.out.println("[videoUploadController] Resolved videoId: " + videoId);
            } catch (IllegalArgumentException e) {
                // S3 key doesn't match expected format (e.g., test uploads without ID)
                System.out.println("[videoUploadController] Skipping file with invalid format: " + s3Key + " - " + e.getMessage());
                // Return 200 to prevent EventBridge retry
                return ResponseEntity.ok("Skipped: Invalid S3 key format");
            }

            // Check if video already processed to avoid duplicate processing
            Optional<Video> existingVideo = videoService.getVideoById(videoId);
            if (existingVideo.isPresent()) {
                Video video = existingVideo.get();
                if (video.getStatus() == VideoStatus.QUEUED || video.getStatus() == VideoStatus.PROCESSING || video.getStatus() == VideoStatus.PROCESSED) {
                    System.out.println("[videoUploadController] Video " + videoId + " already " + video.getStatus() + ", skipping");
                    return ResponseEntity.ok("Video already being processed");
                }
            }

            videoService.markQueued(videoId);
            System.out.println("[videoUploadController] Video marked as QUEUED");
            
            sqsMessageProducer.sendVideoForProcessing(videoId, s3Key);
            System.out.println("[videoUploadController] SQS message sent for processing");

            return ResponseEntity.ok("Video queued for processing.");
            
        } catch (Exception e) {
            // Log error but return 200 to prevent EventBridge retry for non-recoverable errors
            System.err.println("[videoUploadController] Error processing upload event: " + e.getMessage());
            e.printStackTrace();
            
            // Only return 500 for recoverable errors (e.g., temporary DB unavailability)
            // For invalid data/format errors, return 200 to stop retries
            if (isRecoverableError(e)) {
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                        .body("Temporary error, will retry");
            }
            
            return ResponseEntity.ok("Event processed with errors, not retrying");
        }
    }

    // Helper method to determine if error is recoverable
    private boolean isRecoverableError(Exception e) {
        // Examples of recoverable errors that should trigger retry:
        // - Database connection timeout
        // - SQS service unavailable
        // - Network issues
        return e instanceof org.springframework.dao.DataAccessResourceFailureException ||
            e instanceof java.net.SocketTimeoutException ||
            e.getMessage().contains("Connection refused") ||
            e.getMessage().contains("Timeout");
    }


}
