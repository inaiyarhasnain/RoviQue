package com.product.service;

import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

public interface S3Service {

    void upload(String key, MultipartFile file) throws IOException;

    String createObjectUrl(String key);

    String createViewUrl(String objectUrl);

    void deleteObject(String objectUrl);
}
