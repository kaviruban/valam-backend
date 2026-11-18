package com.valam.ecommerce.common.config;

import com.valam.ecommerce.media.StorageProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    private final CorsProperties cors;
    private final StorageProperties storage;

    public WebConfig(CorsProperties cors, StorageProperties storage) {
        this.cors = cors;
        this.storage = storage;
    }

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        String[] origins = cors.allowedOrigins().toArray(String[]::new);
        registry.addMapping("/api/**")
                .allowedOrigins(origins)
                .allowedMethods("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS")
                .exposedHeaders("Location")
                .maxAge(3600);
        registry.addMapping("/uploads/**").allowedOrigins(origins).allowedMethods("GET");
    }

    /** Serves uploaded images. Only files that passed the upload checks are ever written there. */
    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        String location = storage.rootPath().toUri().toString();
        registry.addResourceHandler("/uploads/**")
                .addResourceLocations(location.endsWith("/") ? location : location + "/");
    }
}
