package com.valam.ecommerce.product.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;

/**
 * Payload for creating a product and for fully replacing one (PUT).
 *
 * @param active defaults to true when omitted
 */
public record ProductRequest(
        @NotBlank @Size(max = 255)
        String name,

        @Size(max = 5000)
        String description,

        @NotNull
        Long categoryId,

        @Size(max = 1000)
        String imageUrl,

        Boolean active,

        @NotEmpty @Size(max = 20)
        List<@Valid @NotNull VariantRequest> variants
) {
}
