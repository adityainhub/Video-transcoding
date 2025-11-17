package com.streaming.app.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name="videos")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Video {

    @Id
    @GeneratedValue(strategy=GenerationType.IDENTITY)
    private Long id;

    private String title;

    private String description;

    private String contentType;

    private String s3Key;

    private String fileName;

    @Enumerated(EnumType.STRING)
    private VideoStatus status;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "video_variants", joinColumns = @JoinColumn(name = "video_id"))
    private List<VideoVariant> variants = new ArrayList<>();

    private LocalDateTime uploadedAt;

    private LocalDateTime processedAt;

}

