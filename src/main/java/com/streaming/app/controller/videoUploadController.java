package com.streaming.app.controller;

import com.streaming.app.model.Video;
import com.streaming.app.service.S3Service;
import com.streaming.app.service.VideoService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.Map;

@RestController
@RequestMapping("api/video")
public class videoUploadController {

    @Autowired
    private S3Service s3Service;
    @Autowired
    private VideoService videoService;

    @PostMapping("/upload-url")
    public ResponseEntity<Map<String,String>> getUploadUrl(@RequestBody Map<String,String> request)
    {
        String fileName= request.get("fileName");
        String contentType= request.get("contentType");

        String s3Key = s3Service.generateS3Key(fileName);
        String presignedUrl = s3Service.generatePresignedUrl(s3Key, contentType);

        Video video = videoService.saveUploadedVideo(fileName, s3Key, contentType);

        Map<String,String> response = Map.of(
                "presignedUrl",presignedUrl,
                "s3key",s3Key,
                "videoId", video.getId().toString()
        );
        return ResponseEntity.ok(response);
    }


}
