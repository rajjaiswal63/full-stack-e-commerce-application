package org.ecommerce.project.service;

import org.ecommerce.project.exceptions.APIException;
import org.ecommerce.project.exceptions.ResourceNotFoundException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.core.ResponseBytes;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.core.sync.ResponseTransformer;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.S3Exception;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

@Service
public class FileServiceImpl implements FileService {
    private static final Set<String> ALLOWED_EXTENSIONS = Set.of(".png", ".jpg", ".jpeg", ".webp");
    private static final Set<String> ALLOWED_CONTENT_TYPES = Set.of("image/png", "image/jpeg", "image/jpg", "image/webp");

    private final S3Client s3Client;
    private final String bucketName;
    private final String imagePrefix;
    private final Path legacyImageDirectory;

    public FileServiceImpl(
            S3Client s3Client,
            @Value("${aws.s3.bucket:}") String bucketName,
            @Value("${aws.s3.image-prefix:products}") String imagePrefix,
            @Value("${project.image:./images/products/}") String legacyImagePath) {
        this.s3Client = s3Client;
        this.bucketName = bucketName == null ? "" : bucketName.trim();
        this.imagePrefix = imagePrefix == null ? "products" : imagePrefix.replaceAll("^/+|/+$", "");
        this.legacyImageDirectory = Path.of(legacyImagePath).toAbsolutePath().normalize();
    }

    @Override
    public String uploadImage(MultipartFile file) throws IOException {
        if (file == null || file.isEmpty()) {
            throw new APIException("Choose a non-empty image file.");
        }

        String originalName = file.getOriginalFilename();
        if (originalName == null || originalName.lastIndexOf('.') < 0) {
            throw new APIException("The image filename is invalid.");
        }

        String extension = originalName.substring(originalName.lastIndexOf('.')).toLowerCase(Locale.ROOT);
        String contentType = file.getContentType() == null ? "" : file.getContentType().toLowerCase(Locale.ROOT);
        if (!ALLOWED_EXTENSIONS.contains(extension) || !ALLOWED_CONTENT_TYPES.contains(contentType)) {
            throw new APIException("Supported image types are PNG, JPG, JPEG, and WEBP.");
        }
        if (bucketName.isBlank()) {
            throw new APIException("S3 image storage is not configured. Set AWS_S3_BUCKET.");
        }

        String fileName = UUID.randomUUID() + extension;
        PutObjectRequest request = PutObjectRequest.builder()
                .bucket(bucketName)
                .key(objectKey(fileName))
                .contentType(contentType.equals("image/jpg") ? "image/jpeg" : contentType)
                .build();
        s3Client.putObject(request, RequestBody.fromBytes(file.getBytes()));
        return fileName;
    }

    @Override
    public StoredImage getImage(String fileName) throws IOException {
        if (fileName == null || !fileName.matches("[A-Za-z0-9._-]{1,200}")) {
            throw new ResourceNotFoundException("Product image", "fileName", String.valueOf(fileName));
        }

        if (!bucketName.isBlank()) {
            try {
                ResponseBytes<GetObjectResponse> object = s3Client.getObject(
                        GetObjectRequest.builder()
                                .bucket(bucketName)
                                .key(objectKey(fileName))
                                .build(),
                        ResponseTransformer.toBytes());
                String contentType = object.response().contentType();
                return new StoredImage(object.asByteArray(), contentType == null ? contentTypeFor(fileName) : contentType);
            } catch (S3Exception exception) {
                if (exception.statusCode() != 404) {
                    throw exception;
                }
            }
        }

        return readLegacyLocalImage(fileName);
    }

    private String objectKey(String fileName) {
        return imagePrefix.isBlank() ? fileName : imagePrefix + "/" + fileName;
    }

    private StoredImage readLegacyLocalImage(String fileName) throws IOException {
        Path imagePath = legacyImageDirectory.resolve(fileName).normalize();
        if (!imagePath.startsWith(legacyImageDirectory) || !Files.isRegularFile(imagePath)) {
            throw new ResourceNotFoundException("Product image", "fileName", fileName);
        }

        String contentType = Files.probeContentType(imagePath);
        return new StoredImage(Files.readAllBytes(imagePath), contentType == null ? contentTypeFor(fileName) : contentType);
    }

    private String contentTypeFor(String fileName) {
        String lowerCaseName = fileName.toLowerCase(Locale.ROOT);
        if (lowerCaseName.endsWith(".png")) return "image/png";
        if (lowerCaseName.endsWith(".jpg") || lowerCaseName.endsWith(".jpeg")) return "image/jpeg";
        if (lowerCaseName.endsWith(".webp")) return "image/webp";
        if (lowerCaseName.endsWith(".svg")) return "image/svg+xml";
        return "application/octet-stream";
    }
}