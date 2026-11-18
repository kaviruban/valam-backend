package com.valam.ecommerce.media;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.nio.file.Path;

/** @param localDir folder where uploaded files are saved; relative paths resolve against the working directory */
@ConfigurationProperties("app.storage")
public record StorageProperties(String localDir) {

    public StorageProperties {
        if (localDir == null || localDir.isBlank()) {
            localDir = "uploads";
        }
    }

    public Path rootPath() {
        return Path.of(localDir).toAbsolutePath().normalize();
    }
}
