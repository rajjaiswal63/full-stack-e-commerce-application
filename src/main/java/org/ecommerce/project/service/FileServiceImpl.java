package org.ecommerce.project.service;

import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.UUID;
@Service
public class FileServiceImpl implements FilesService {
    @Override
    public String uploadImage(String path, MultipartFile image) {
        //file name of the original file
        String originalFileName=image.getOriginalFilename();
        //Generate unique image name
        String randomId= UUID.randomUUID().toString();
        //mat.jpg ->1234-> 1234.jpg
        String fileName = randomId.concat(originalFileName.substring(originalFileName.lastIndexOf(".")));
        String filePath=path+ File.separator+fileName;

        //check if the directory exists and create
        File directory=new File(path);
        if(!directory.exists()){
            directory.mkdirs();
        }

        //upload to server
        try {
            Files.copy(image.getInputStream(), Paths.get(filePath));
        } catch (Exception e) {
            e.printStackTrace();
        }
        return fileName;
    }
}

