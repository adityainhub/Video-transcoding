package com.streaming.app.repository;

import com.streaming.app.model.Video;
import com.streaming.app.model.VideoStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface VideoRepository extends JpaRepository<Video, Long> {

    List<Video> findByStatus(VideoStatus status);
    List<Video> findByStatusOrderByUploadedAtAsc(VideoStatus status);

    Optional<Video> findByS3Key(String s3Key);

    Optional<Video> findByFileName(String originalFileName);
}
