package com.product.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.GetUrlRequest;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;

import java.io.IOException;
import java.net.URI;
import java.time.Duration;

// Register this class as the Spring service that works with Amazon S3.
@Service
public class S3ServiceImpl implements S3Service {

    private static final Duration URL_VALIDITY = Duration.ofMinutes(10);

    private final S3Client s3Client;
    private final S3Presigner s3Presigner;
    private final String bucket;

    public S3ServiceImpl(
            S3Client s3Client,
            S3Presigner s3Presigner,
            // Read the S3 bucket name from Spring configuration.
            @Value("${aws.bucket-name}") String bucket) {
        this.s3Client = s3Client;
        this.s3Presigner = s3Presigner;
        this.bucket = bucket;
    }

    // Implement the file-upload operation declared by S3Service.
    @Override
    public void upload(String key, MultipartFile file) throws IOException {
        validateBucket();
        try (var inputStream = file.getInputStream()) {
            // S3 stores the file bytes; the database saves the resulting object URL.
            s3Client.putObject(
                    PutObjectRequest.builder()
                            .bucket(bucket)
                            .key(key)
                            .contentType(file.getContentType())
                            .build(),
                    RequestBody.fromInputStream(inputStream, file.getSize())
            );
        }
    }

    // Implement the operation that builds the permanent S3 object address.
    @Override
    public String createObjectUrl(String key) {
        validateBucket();
        return s3Client.utilities().getUrl(GetUrlRequest.builder()
                .bucket(bucket)
                .key(key)
                .build()).toExternalForm();
    }

    // Implement the operation that creates a temporary file-view link.
    @Override
    public String createViewUrl(String objectUrl) {
        validateBucket();
        String key = extractKey(objectUrl);
        // The bucket stays private; this URL grants temporary access to one object.
        GetObjectRequest getRequest = GetObjectRequest.builder()
                .bucket(bucket)
                .key(key)
                .build();
        GetObjectPresignRequest presignRequest = GetObjectPresignRequest.builder()
                .signatureDuration(URL_VALIDITY)
                .getObjectRequest(getRequest)
                .build();
        return s3Presigner.presignGetObject(presignRequest).url().toString();
    }

    @Override
    public void deleteObject(String objectUrl) {
        validateBucket();
        s3Client.deleteObject(DeleteObjectRequest.builder()
                .bucket(bucket)
                .key(extractKey(objectUrl))
                .build());
    }

    private String extractKey(String objectUrl) {
        URI uri = URI.create(objectUrl);
        String path = uri.getPath();
        if (!uri.isAbsolute()) {
            // Support keys saved by earlier versions of the upload code.
            return objectUrl;
        }
        if (path == null || path.length() < 2) {
            throw new IllegalArgumentException("Invalid S3 object URL");
        }
        return path.substring(1);
    }

    private void validateBucket() {
        if (bucket.isBlank()) {
            throw new ResponseStatusException(
                    HttpStatus.SERVICE_UNAVAILABLE,
                    "S3 is not configured. Set the AWS_S3_BUCKET environment variable.");
        }
    }
}
