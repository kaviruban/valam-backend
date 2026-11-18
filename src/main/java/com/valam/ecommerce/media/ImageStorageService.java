package com.valam.ecommerce.media;

import com.valam.ecommerce.common.exception.BusinessValidationException;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Arrays;
import java.util.UUID;

/**
 * Saves product images on the local disk. The file type is decided from the file's own bytes, never from the
 * client's file name or Content-Type, and files get random names, so an upload cannot overwrite anything or be
 * served as HTML/script.
 * <p>
 * For production, swap this for object storage (S3, Cloudinary, ...) behind the same method.
 */
@Service
public class ImageStorageService {

    static final String FOLDER = "images";

    private final Path imagesDir;

    public ImageStorageService(StorageProperties properties) {
        this.imagesDir = properties.rootPath().resolve(FOLDER);
    }

    /** @return the stored file's path relative to the public {@code /uploads/} prefix, e.g. "images/ab12.jpg" */
    public String store(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BusinessValidationException("Choose an image file to upload.");
        }
        String extension = detectExtension(file);
        String name = UUID.randomUUID() + "." + extension;
        try (InputStream in = file.getInputStream()) {
            Files.createDirectories(imagesDir);
            Files.copy(in, imagesDir.resolve(name), StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            throw new UncheckedIOException("Could not save uploaded image", e);
        }
        return FOLDER + "/" + name;
    }

    private static String detectExtension(MultipartFile file) {
        byte[] head = new byte[12];
        int read;
        try (InputStream in = file.getInputStream()) {
            read = in.readNBytes(head, 0, head.length);
        } catch (IOException e) {
            throw new UncheckedIOException("Could not read uploaded image", e);
        }
        if (read >= 3 && (head[0] & 0xFF) == 0xFF && (head[1] & 0xFF) == 0xD8 && (head[2] & 0xFF) == 0xFF) {
            return "jpg";
        }
        if (read >= 8 && Arrays.equals(Arrays.copyOf(head, 8),
                new byte[]{(byte) 0x89, 'P', 'N', 'G', '\r', '\n', 0x1A, '\n'})) {
            return "png";
        }
        if (read >= 12 && head[0] == 'R' && head[1] == 'I' && head[2] == 'F' && head[3] == 'F'
                && head[8] == 'W' && head[9] == 'E' && head[10] == 'B' && head[11] == 'P') {
            return "webp";
        }
        throw new BusinessValidationException("Only JPEG, PNG and WebP images are allowed.");
    }
}
