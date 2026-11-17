package com.product.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider;
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;

// Tell Spring this class provides settings and objects used by the application.
@Configuration
public class S3Config {

    // Create an AWS credentials provider for the S3 client and signer.
    @Bean
    public AwsCredentialsProvider awsCredentialsProvider(
            // Read an optional AWS access key from Spring configuration.
            @Value("${aws.access-key:}") String accessKey,
            // Read an optional AWS secret key from Spring configuration.
            @Value("${aws.secret-key:}") String secretKey) {
        if (accessKey.isBlank() != secretKey.isBlank()) {
            throw new IllegalStateException(
                    "Configure both AWS_ACCESS_KEY_ID and AWS_SECRET_ACCESS_KEY, or neither.");
        }
        if (!accessKey.isBlank()) {
            return StaticCredentialsProvider.create(
                    AwsBasicCredentials.create(accessKey, secretKey));
        }
        return DefaultCredentialsProvider.create();
    }

    // Create the S3 client as a Spring-managed object.
    @Bean
    public S3Client s3Client(
            // Read the AWS region from Spring configuration.
            @Value("${aws.region}") String region,
            AwsCredentialsProvider credentialsProvider) {
        return S3Client.builder()
                .region(Region.of(region))
                .credentialsProvider(credentialsProvider)
                .build();
    }

    // Create a signer that makes temporary links to private S3 files.
    @Bean
    public S3Presigner s3Presigner(
            // Read the AWS region from Spring configuration.
            @Value("${aws.region}") String region,
            AwsCredentialsProvider credentialsProvider) {
        return S3Presigner.builder()
                .region(Region.of(region))
                .credentialsProvider(credentialsProvider)
                .build();
    }

}
