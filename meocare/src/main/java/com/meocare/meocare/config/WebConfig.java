package com.meocare.meocare.config;

import java.nio.file.Path;
import java.nio.file.Paths;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    private static final Logger log = LoggerFactory.getLogger(WebConfig.class);

    private final Path uploadDir;

    public WebConfig(@Value("${app.upload.dir:./uploads}") String uploadDir) {
        String configured = (uploadDir == null || uploadDir.isBlank()) ? "./uploads" : uploadDir.trim();
        this.uploadDir = Paths.get(configured).toAbsolutePath().normalize();
    }

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        String location = uploadDir.toUri().toString();
        if (!location.endsWith("/")) {
            location = location + "/";
        }
        log.info("Exposing /uploads/** from directory: {}", uploadDir);
        registry.addResourceHandler("/uploads/**")
                .addResourceLocations(location);
    }
}
