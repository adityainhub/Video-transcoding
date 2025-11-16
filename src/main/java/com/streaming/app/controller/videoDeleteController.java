package com.streaming.app.controller;

import com.streaming.app.service.S3Service;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.RequestParam;

public class videoDeleteController {

    @Autowired
    private S3Service s3Service;


    @DeleteMapping("/delete")
    public ResponseEntity<String> deleteVideo(@RequestParam String s3Key)
    {
        s3Service.deleteFile(s3Key);
        return ResponseEntity.ok(" File Successfully deleted " + s3Key);
    }
}
