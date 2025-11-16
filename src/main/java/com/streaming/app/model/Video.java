package com.streaming.app.model;


import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

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

    private LocalDateTime uploadedAt;

    private LocalDateTime processedAt;

}

