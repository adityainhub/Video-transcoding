package com.streaming.app.service;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.sqs.SqsClient;
import software.amazon.awssdk.services.sqs.model.SendMessageRequest;

@Component
@RequiredArgsConstructor
public class SqsMessageProducer {

    private final SqsClient sqsClient;

    @Value("${aws.sqs.videoQueueUrl}")
    private String queueUrl;

    public void sendVideoForProcessing(Long videoId, String s3Key) {
        System.out.println("[SqsMessageProducer] sendVideoForProcessing - videoId: " + videoId + ", s3Key: " + s3Key);
        String messageBody = String.format("{\"videoId\": %d, \"s3Key\": \"%s\"}", videoId, s3Key);

        SendMessageRequest request = SendMessageRequest.builder()
                .queueUrl(queueUrl)
                .messageBody(messageBody)
                .build();

        sqsClient.sendMessage(request);
        System.out.println("[SqsMessageProducer] SQS message sent → " + messageBody);
    }
}