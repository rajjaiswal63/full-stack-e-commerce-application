package org.ecommerce.project.controller;

import org.ecommerce.project.service.FileService;
import org.ecommerce.project.service.StoredImage;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.time.Duration;

@RestController
public class ProductImageController {
    private final FileService fileService;

    public ProductImageController(FileService fileService) {
        this.fileService = fileService;
    }

    @GetMapping("/images/products/{fileName:.+}")
    public ResponseEntity<byte[]> getProductImage(@PathVariable String fileName) throws IOException {
        StoredImage image = fileService.getImage(fileName);
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(image.contentType()))
                .header(HttpHeaders.CACHE_CONTROL, CacheControl.maxAge(Duration.ofDays(1)).cachePublic().getHeaderValue())
                .body(image.bytes());
    }
}