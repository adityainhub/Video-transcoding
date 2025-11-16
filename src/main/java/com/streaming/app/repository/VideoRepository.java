package com.streaming.app.repository;

import com.streaming.app.model.Video;
import com.streaming.app.model.VideoStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface VideoRepository extends JpaRepository<Video, Long> {

    List<Video> findByStatus(VideoStatus status);
    List<Video> findByStatusOrderByUploadedAtAsc(VideoStatus status);
}
