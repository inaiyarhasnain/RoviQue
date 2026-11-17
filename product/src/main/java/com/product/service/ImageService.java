package com.product.service;

import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

public interface ImageService {

    List<String> uploadBrandFiles(Integer brandId, List<MultipartFile> files) throws IOException;

    List<String> getBrandFileUrls(Integer brandId);

    String createViewUrl(String objectUrl);

    void deleteBrandFiles(Integer brandId);
}
