package org.ecommerce.project.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.nio.file.Path;

@Configuration
public class WebMvcConfig implements WebMvcConfigurer {
    private final String imageStoragePath;

    public WebMvcConfig(@Value("${project.image:./images/products/}") String imageStoragePath) {
        this.imageStoragePath = imageStoragePath;
    }

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        Path imageDirectory = Path.of(imageStoragePath).toAbsolutePath().normalize();
        Path publicImageRoot = imageDirectory.getParent() == null ? imageDirectory : imageDirectory.getParent();
        String location = publicImageRoot.toUri().toString();
        if (!location.endsWith("/")) {
            location += "/";
        }
        registry.addResourceHandler("/images/**").addResourceLocations(location);
    }
}
