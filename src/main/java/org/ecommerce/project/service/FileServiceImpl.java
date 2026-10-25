package org.ecommerce.project.service;

import org.ecommerce.project.exceptions.APIException;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

@Service
public class FileServiceImpl implements FileService {
    private static final Set<String> ALLOWED_EXTENSIONS = Set.of(".png", ".jpg", ".jpeg", ".webp");

    @Override
    public String uploadImage(String path, MultipartFile file) throws IOException {
        if (file == null || file.isEmpty()) {
            throw new APIException("Choose a non-empty image file.");
        }
        String originalName = file.getOriginalFilename();
        if (originalName == null || originalName.lastIndexOf('.') < 0) {
            throw new APIException("The image filename is invalid.");
        }
        String extension = originalName.substring(originalName.lastIndexOf('.')).toLowerCase(Locale.ROOT);
        if (!ALLOWED_EXTENSIONS.contains(extension)
                || file.getContentType() == null || !file.getContentType().toLowerCase(Locale.ROOT).startsWith("image/")) {
            throw new APIException("Supported image types are PNG, JPG, JPEG, and WEBP.");
        }

        Path directory = Path.of(path).toAbsolutePath().normalize();
        Files.createDirectories(directory);
        String fileName = UUID.randomUUID() + extension;
        Path destination = directory.resolve(fileName).normalize();
        if (!destination.startsWith(directory)) {
            throw new APIException("Invalid image path.");
        }
        try (var input = file.getInputStream()) {
            Files.copy(input, destination);
        }
        return fileName;
    }
}
