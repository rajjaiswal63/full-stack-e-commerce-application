package org.ecommerce.project.service;

import org.springframework.web.multipart.MultipartFile;

public interface FilesService {

    String uploadImage(String path, MultipartFile image);
}
