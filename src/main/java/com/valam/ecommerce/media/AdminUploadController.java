package com.valam.ecommerce.media;

import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;

/**
 * Image upload for the admin panel: upload first, then put the returned {@code url} into a product's
 * {@code images}. TODO: secure with ADMIN role once auth is added.
 */
@RestController
@RequestMapping("/api/v1/admin/uploads")
public class AdminUploadController {

    public record UploadResponse(String url) {
    }

    private final ImageStorageService storage;

    public AdminUploadController(ImageStorageService storage) {
        this.storage = storage;
    }

    /** multipart/form-data with a single "file" part; JPEG, PNG or WebP up to 5 MB. */
    @PostMapping(path = "/images", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<UploadResponse> uploadImage(@RequestParam("file") MultipartFile file) {
        String path = storage.store(file);
        URI url = ServletUriComponentsBuilder.fromCurrentContextPath().path("/uploads/" + path).build().toUri();
        return ResponseEntity.created(url).body(new UploadResponse(url.toString()));
    }
}
