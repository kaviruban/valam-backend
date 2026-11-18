package com.valam.ecommerce.common.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.List;

/** @param allowedOrigins browser origins allowed to call the API, e.g. the admin panel's dev server */
@ConfigurationProperties("app.cors")
public record CorsProperties(List<String> allowedOrigins) {

    public CorsProperties {
        allowedOrigins = allowedOrigins == null ? List.of() : List.copyOf(allowedOrigins);
    }
}
