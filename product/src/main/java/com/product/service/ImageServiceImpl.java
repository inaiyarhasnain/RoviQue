package com.product.service;

import com.product.entity.Brand;
import com.product.entity.Image;
import com.product.repository.BrandRepository;
import com.product.repository.ImageRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

// Register this class as the Spring service that handles image operations.
@Service
public class ImageServiceImpl implements ImageService {

    private static final Map<String, String> FILE_EXTENSIONS = Map.ofEntries(
            Map.entry("image/jpeg", ".jpg"),
            Map.entry("image/png", ".png"),
            Map.entry("image/gif", ".gif"),
            Map.entry("image/webp", ".webp"),
            Map.entry("application/pdf", ".pdf"),
            Map.entry("text/plain", ".txt"),
            Map.entry("text/csv", ".csv"),
            Map.entry("application/msword", ".doc"),
            Map.entry("application/vnd.openxmlformats-officedocument.wordprocessingml.document", ".docx"),
            Map.entry("application/vnd.ms-excel", ".xls"),
            Map.entry("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", ".xlsx"),
            Map.entry("application/vnd.ms-powerpoint", ".ppt"),
            Map.entry("application/vnd.openxmlformats-officedocument.presentationml.presentation", ".pptx")
    );

    private final S3Service s3Service;
    private final BrandRepository brandRepository;
    private final ImageRepository imageRepository;

    public ImageServiceImpl(
            S3Service s3Service,
            BrandRepository brandRepository,
            ImageRepository imageRepository) {
        this.s3Service = s3Service;
        this.brandRepository = brandRepository;
        this.imageRepository = imageRepository;
    }

    // Implement the image upload operation declared by ImageService.
    @Override
    public List<String> uploadBrandFiles(Integer brandId, List<MultipartFile> files) throws IOException {
        // Stop early if the request has no files.
        if (files == null || files.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Select at least one image");
        }

        // Check the brand first so files cannot be attached to a missing brand.
        Brand brand = brandRepository.findById(brandId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Brand not found: " + brandId));

        // Validate every file before uploading any of them.
        List<String> extensions = validateFiles(files);
        List<String> viewUrls = new ArrayList<>(files.size());
        for (int i = 0; i < files.size(); i++) {
            MultipartFile file = files.get(i);
            // Give each upload a unique storage name to avoid replacing another file.
            String key = "brands/" + brandId + "/" + UUID.randomUUID() + extensions.get(i);
            s3Service.upload(key, file);

            // Save the permanent object location so a fresh view link can be made later.
            Image image = new Image();
            // Save the stable S3 object URL, not the expiring signed URL.
            image.setUrl(s3Service.createObjectUrl(key));
            image.setBrand(brand);
            imageRepository.save(image);
            viewUrls.add(s3Service.createViewUrl(image.getUrl()));
        }
        return viewUrls;
    }

    // Implement the operation that returns view links for a brand's files.
    @Override
    public List<String> getBrandFileUrls(Integer brandId) {
        if (!brandRepository.existsById(brandId)) {
            throw new ResponseStatusException(
                    HttpStatus.NOT_FOUND, "Brand not found: " + brandId);
        }
        return imageRepository.findAllByBrand_Id(brandId).stream()
                .map(image -> s3Service.createViewUrl(image.getUrl()))
                .toList();
    }

    // Implement the operation that creates a temporary viewing link.
    @Override
    public String createViewUrl(String objectUrl) {
        return s3Service.createViewUrl(objectUrl);
    }

    @Override
    public void deleteBrandFiles(Integer brandId) {
        Brand brand = brandRepository.findById(brandId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Brand not found: " + brandId));
        List<Image> images = imageRepository.findAllByBrand_Id(brandId);
        for (Image image : images) {
            s3Service.deleteObject(image.getUrl());
        }
        brand.getImages().removeAll(images);
        imageRepository.deleteAll(images);
    }

    private List<String> validateFiles(List<MultipartFile> files) {
        List<String> extensions = new ArrayList<>(files.size());
        for (MultipartFile file : files) {
            if (file == null || file.isEmpty()) {
                throw new ResponseStatusException(
                        HttpStatus.BAD_REQUEST, "One of the image files is empty");
            }

            String contentType = file.getContentType();
            String extension = contentType == null
                    ? null
                    : FILE_EXTENSIONS.get(contentType.toLowerCase(Locale.ROOT));
            if (extension == null) {
                throw new ResponseStatusException(
                        HttpStatus.BAD_REQUEST,
                        "Supported files: JPEG, PNG, GIF, WEBP, PDF, TXT, CSV, DOC/DOCX, XLS/XLSX, PPT/PPTX");
            }
            extensions.add(extension);
        }
        return extensions;
    }
}
