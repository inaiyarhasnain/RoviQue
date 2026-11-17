package com.product.controller;

import com.product.dto.ApiResponse;
import com.product.service.ImageService;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartHttpServletRequest;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

// Tell Spring this class handles HTTP requests for image operations.
@RestController
// Add this URL prefix to all image endpoints.
@RequestMapping("/api/v1/product")
public class ImageController {

    private final ImageService imageService;

    public ImageController(ImageService imageService) {
        this.imageService = imageService;
    }

    // Accept a multipart form upload for either supported brand-file URL.
    @PostMapping(
            value = {
                    "/brands/{brandId}/images",
                    "/brands/{brandId}/files"
            },
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<List<String>>> uploadBrandFiles(
            // Read the brand ID from the URL.
            @PathVariable Integer brandId,
            MultipartHttpServletRequest request) throws IOException {
        // Accept file parts even when the multipart field name differs.
        // Gather all uploaded files, then pass them to the image service.
        List<MultipartFile> uploads = request.getMultiFileMap().values().stream()
                .flatMap(List::stream)
                .toList();
        List<String> urls = imageService.uploadBrandFiles(brandId, uploads);
        return response("Files uploaded successfully", urls, HttpStatus.CREATED);
    }

    // Map GET requests to retrieving a brand's file-view links.
    @GetMapping({
            "/brands/{brandId}/images",
            "/brands/{brandId}/files"
    })
    public ResponseEntity<ApiResponse<List<String>>> getBrandFileUrls(
            // Read the brand ID from the URL.
            @PathVariable Integer brandId) {
        // Ask the service for temporary links to the brand's stored files.
        List<String> urls = imageService.getBrandFileUrls(brandId);
        return response("Brand file URLs fetched successfully", urls, HttpStatus.OK);
    }

    private ResponseEntity<ApiResponse<List<String>>> response(
            String message, List<String> urls, HttpStatus status) {
        ApiResponse<List<String>> response = new ApiResponse<>();
        response.setMessage(message);
        response.setStatus(status.value());
        response.setData(urls);
        return ResponseEntity.status(status).body(response);
    }
}
