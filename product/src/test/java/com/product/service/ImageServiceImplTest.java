package com.product.service;

import com.product.entity.Brand;
import com.product.entity.Image;
import com.product.repository.BrandRepository;
import com.product.repository.ImageRepository;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ImageServiceImplTest {

    // Tell JUnit to run this image-upload behavior check as a test.
    @Test
    void uploadsFileAndSavesItsStableS3Url() throws Exception {
        // Use mocks so the test does not contact AWS or a real database.
        S3Service s3Service = mock(S3Service.class);
        BrandRepository brandRepository = mock(BrandRepository.class);
        ImageRepository imageRepository = mock(ImageRepository.class);
        Brand brand = new Brand();
        brand.setId(7);
        when(brandRepository.findById(7)).thenReturn(Optional.of(brand));
        when(s3Service.createObjectUrl(any())).thenReturn(
                "https://rovique.s3.eu-north-1.amazonaws.com/brands/7/file.pdf");
        when(s3Service.createViewUrl(any())).thenReturn("https://signed.example.test/file");
        when(imageRepository.save(any(Image.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // Run the upload operation and check its result and saved image.
        ImageService service = new ImageServiceImpl(s3Service, brandRepository, imageRepository);
        MockMultipartFile file = new MockMultipartFile(
                "files", "file.pdf", "application/pdf", "sample".getBytes());

        List<String> urls = service.uploadBrandFiles(7, List.of(file));

        assertEquals(List.of("https://signed.example.test/file"), urls);
        verify(s3Service).upload(any(), any());
        verify(imageRepository).save(org.mockito.ArgumentMatchers.argThat(
                image -> image.getUrl().equals(
                        "https://rovique.s3.eu-north-1.amazonaws.com/brands/7/file.pdf")
                        && image.getBrand() == brand));
    }
}
