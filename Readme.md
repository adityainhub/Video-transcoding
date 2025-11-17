# Video Transcoding System

A serverless video transcoding pipeline built with AWS ECS Fargate, Lambda, and Spring Boot. Automatically transcodes uploaded videos into multiple quality levels (1080p, 720p, 480p, 360p) and generates thumbnails.

## 🏗️ Architecture Overview

```
┌─────────────┐
│   User/     │
│  Frontend   │
└──────┬──────┘
       │ 1. Request presigned URL
       │ 2. Upload video directly
       ↓
┌─────────────────────────────────┐
│   S3 Raw Bucket                 │
│   video-transcoding-raw-videos  │
│   Path: raw-videos/{id}/file.mp4│
└────────┬────────────────────────┘
         │
         │ S3 Event Notification
         ↓
    ┌────────────┐
    │ EventBridge│
    └─────┬──────┘
          │
          ↓
┌──────────────────────┐
│   Spring Boot        │
│   Backend (Azure)    │
│   - Creates DB entry │
│   - Publishes to SQS │
└──────────┬───────────┘
           │
           ↓
┌─────────────────────┐
│   SQS Queue         │
│   Message:          │
│   {videoId, s3Key}  │
└─────────┬───────────┘
          │
          │ Trigger
          ↓
┌──────────────────────┐
│   Lambda Function    │
│   video-transcoder-  │
│   launcher           │
│   - Launches ECS     │
└──────────┬───────────┘
           │
           │ RunTask
           ↓
┌───────────────────────────────┐
│   ECS Fargate Task (ARM64)    │
│   video-transcoder            │
│   - Download from S3          │
│   - Transcode (FFmpeg)        │
│   - Upload variants           │
│   - Send callbacks            │
└────────┬──────────────────────┘
         │
         │ Upload outputs
         ↓
┌─────────────────────────────────┐
│   S3 Processed Bucket           │
│   video-transcoding-processed   │
│   Path: processed-videos/{id}/  │
│   - 1080p.mp4                   │
│   - 720p.mp4                    │
│   - 480p.mp4                    │
│   - 360p.mp4                    │
│   - thumb.jpg                   │
└─────────────────────────────────┘
         │
         │ Callbacks (signed)
         ↓
┌──────────────────────┐
│   Spring Boot        │
│   Backend            │
│   - /processing      │
│   - /completed       │
│   - /failed          │
└──────────┬───────────┘
           │
           ↓
┌──────────────────────┐
│   PostgreSQL         │
│   - videos table     │
│   - video_variants   │
└──────────────────────┘
```

## 📋 Features

- ✅ **Serverless Architecture** - Auto-scaling with ECS Fargate
- ✅ **Multiple Quality Levels** - 1080p, 720p, 480p, 360p
- ✅ **Thumbnail Generation** - Automatic thumbnail extraction
- ✅ **Secure Callbacks** - HMAC-SHA256 signed webhooks
- ✅ **Status Tracking** - Real-time video processing status (QUEUED → PROCESSING → PROCESSED)
- ✅ **ARM64 Optimized** - Cost-effective ARM architecture
- ✅ **Database Persistence** - PostgreSQL for video metadata and variants

## 🔄 Detailed Flow

### 1. Video Upload
```
Frontend → Backend: POST /api/videos/presigned-url
Backend → Frontend: { uploadUrl, videoId, s3Key }
Frontend → S3: PUT to presigned URL
```

### 2. Event Detection
```
S3 → EventBridge: Object Created event
EventBridge → Backend: POST /api/video/videos/uploaded
Backend → Database: Create video record (status: UPLOADED)
Backend → SQS: Publish { videoId: 123, s3Key: "raw-videos/123/video.mp4" }
```

### 3. Transcoding Trigger
```
SQS → Lambda: Event with video metadata
Lambda → ECS: RunTask with environment overrides:
  - VIDEO_ID=123
  - S3_VIDEO_KEY=raw-videos/123/video.mp4
  - CALLBACK_BASE_URL=https://backend.example.com/api/videos
  - ECS_CALLBACK_SECRET=<secret>
```

### 4. Transcoding Process
```
ECS Task:
1. Download: S3_RAW_BUCKET/raw-videos/123/video.mp4
2. Callback: POST /api/videos/123/processing (status: PROCESSING)
3. Transcode: FFmpeg generates 4 quality levels + thumbnail
4. Upload: All outputs to S3_PROCESSED_BUCKET/processed-videos/123/
5. Callback: POST /api/videos/123/completed (with variants metadata)
```

### 5. Database Update
```
Backend:
1. Validates HMAC signature (X-ECS-Signature header)
2. Saves variants to database
3. Updates video status to PROCESSED
4. Sets processedAt timestamp
```

## 🛠️ Technology Stack

### AWS Services
- **Amazon S3** - Raw and processed video storage
- **Amazon SQS** - Message queue for decoupling
- **AWS Lambda** - ECS task orchestration
- **Amazon ECS Fargate** - Container-based video transcoding
- **Amazon ECR** - Docker image registry
- **Amazon EventBridge** - S3 event routing
- **Amazon CloudWatch** - Logging and monitoring

### Backend
- **Spring Boot** - REST API and business logic
- **PostgreSQL** - Video metadata and variants
- **Azure App Service** - Backend hosting

### Transcoding Container
- **Node.js 18** - Runtime environment
- **FFmpeg** - Video transcoding engine
- **AWS SDK v3** - S3 operations
- **Axios** - HTTP callbacks
