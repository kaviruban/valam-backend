package com.valam.ecommerce.product.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.util.List;

/**
 * Payload for creating a product and for fully replacing one (PUT). Optional fields left out are cleared on PUT.
 *
 * @param slug            storefront URL part; generated from the name when omitted on create, kept as-is when
 *                        omitted on update
 * @param images          gallery in display order; the first is the main image
 * @param highlights      short bullet points, e.g. "No preservatives"
 * @param countryOfOrigin defaults to "India" when omitted
 * @param hsnCode         4, 6 or 8 digit HSN code for GST invoices
 * @param gstRate         GST percentage included in the prices, e.g. 5 or 18
 * @param active          defaults to true when omitted; inactive products are hidden from the store
 * @param featured        defaults to false when omitted
 * @param version         on update, the {@code version} the admin loaded; if someone saved in between, the update
 *                        is rejected with 409 instead of overwriting their changes. Ignored on create.
 */
public record ProductRequest(
        @NotBlank @Size(max = 255)
        String name,

        @Size(max = 200)
        @Pattern(regexp = "^[a-z0-9]+(-[a-z0-9]+)*$",
                message = "must be lowercase letters, digits and single hyphens, e.g. 'groundnut-oil'")
        String slug,

        @Size(max = 500)
        String shortDescription,

        @Size(max = 10000)
        String description,

        @NotNull
        Long categoryId,

        @Size(max = 10)
        List<@Valid @NotNull ProductImageDto> images,

        @Size(max = 10)
        List<@NotBlank @Size(max = 255) String> highlights,

        @Size(max = 5000)
        String ingredients,

        @Size(max = 5000)
        String usageInstructions,

        @Size(max = 2000)
        String storageInstructions,

        @Size(max = 100)
        String shelfLife,

        @Size(max = 100)
        String countryOfOrigin,

        @Pattern(regexp = "^\\d{4}(\\d{2}){0,2}$", message = "must be a 4, 6 or 8 digit HSN code")
        String hsnCode,

        @DecimalMin("0") @DecimalMax("100") @Digits(integer = 3, fraction = 2)
        BigDecimal gstRate,

        @Size(max = 100)
        String metaTitle,

        @Size(max = 300)
        String metaDescription,

        Boolean active,

        Boolean featured,

        Long version,

        @NotEmpty @Size(max = 20)
        List<@Valid @NotNull VariantRequest> variants
) {
}
