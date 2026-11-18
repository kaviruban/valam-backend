package com.valam.ecommerce.product.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * A gallery image, in requests and responses.
 *
 * @param url     absolute http(s) URL, e.g. one returned by {@code POST /api/v1/admin/uploads/images}
 * @param altText describes the image for screen readers and search engines
 */
public record ProductImageDto(
        @NotBlank @Size(max = 1000) @Pattern(regexp = "^https?://\\S+$", message = "must be an http(s) URL")
        String url,

        @Size(max = 255)
        String altText
) {
}
