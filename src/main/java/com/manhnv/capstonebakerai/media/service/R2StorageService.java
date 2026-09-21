package com.manhnv.capstonebakerai.media.service;

import com.manhnv.capstonebakerai.config.R2Properties;
import com.manhnv.capstonebakerai.media.enums.R2BucketType;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.HeadObjectRequest;
import software.amazon.awssdk.services.s3.model.NoSuchKeyException;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.S3Exception;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;
import software.amazon.awssdk.services.s3.presigner.model.PutObjectPresignRequest;

import java.net.URL;
import java.time.Duration;

@Service
@RequiredArgsConstructor
public class R2StorageService {

    private final S3Client r2Client;
    private final S3Presigner r2Presigner;
    private final R2Properties properties;

    public void upload(R2BucketType bucketType, String objectKey, byte[] content, String contentType) {
        PutObjectRequest request = PutObjectRequest.builder()
                .bucket(bucketName(bucketType))
                .key(objectKey)
                .contentType(contentType)
                .build();

        r2Client.putObject(request, RequestBody.fromBytes(content));
    }

    public void delete(R2BucketType bucketType, String objectKey) {
        r2Client.deleteObject(DeleteObjectRequest.builder()
                .bucket(bucketName(bucketType))
                .key(objectKey)
                .build());
    }

    public boolean exists(R2BucketType bucketType, String objectKey) {
        try {
            r2Client.headObject(HeadObjectRequest.builder()
                    .bucket(bucketName(bucketType))
                    .key(objectKey)
                    .build());
            return true;
        } catch (NoSuchKeyException exception) {
            return false;
        } catch (S3Exception exception) {
            if (exception.statusCode() == 404) {
                return false;
            }
            throw exception;
        }
    }

    public URL createUploadUrl(R2BucketType bucketType, String objectKey, String contentType) {
        return createUploadUrl(bucketType, objectKey, contentType, properties.presignedUrlDuration());
    }

    public URL createUploadUrl(
            R2BucketType bucketType,
            String objectKey,
            String contentType,
            Duration duration
    ) {
        PutObjectRequest objectRequest = PutObjectRequest.builder()
                .bucket(bucketName(bucketType))
                .key(objectKey)
                .contentType(contentType)
                .build();

        return r2Presigner.presignPutObject(PutObjectPresignRequest.builder()
                        .signatureDuration(duration)
                        .putObjectRequest(objectRequest)
                        .build())
                .url();
    }

    public URL createDownloadUrl(R2BucketType bucketType, String objectKey) {
        return createDownloadUrl(bucketType, objectKey, properties.presignedUrlDuration());
    }

    public URL createDownloadUrl(R2BucketType bucketType, String objectKey, Duration duration) {
        return r2Presigner.presignGetObject(GetObjectPresignRequest.builder()
                        .signatureDuration(duration)
                        .getObjectRequest(request -> request
                                .bucket(bucketName(bucketType))
                                .key(objectKey))
                        .build())
                .url();
    }

    public String publicUrl(String objectKey) {
        String baseUrl = properties.publicUrl();
        if (baseUrl == null || baseUrl.isBlank()) {
            throw new IllegalStateException("R2 public URL is not configured");
        }
        return baseUrl.replaceAll("/+$", "") + "/" + objectKey;
    }

    private String bucketName(R2BucketType bucketType) {
        return switch (bucketType) {
            case PUBLIC -> properties.publicBucket();
            case PRIVATE -> properties.privateBucket();
        };
    }
}
