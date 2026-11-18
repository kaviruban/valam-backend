package com.valam.ecommerce.product.dto;

import com.valam.ecommerce.product.model.MeasureType;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * @param measureType  the category's measure type, so the edit form can offer the right units
 * @param thumbnailUrl the main (first) image, or null when the product has no images
 * @param minPrice     lowest variant price, for "from ₹80" displays
 * @param totalStock   sum of stock across all variants
 * @param inStock      true when at least one variant has stock
 * @param version      send back unchanged on update to detect conflicting edits
 */
public record ProductResponse(
        Long id,
        String name,
        String slug,
        String shortDescription,
        String description,
        Long categoryId,
        String categoryName,
        MeasureType measureType,
        List<ProductImageDto> images,
        String thumbnailUrl,
        List<String> highlights,
        String ingredients,
        String usageInstructions,
        String storageInstructions,
        String shelfLife,
        String countryOfOrigin,
        String hsnCode,
        BigDecimal gstRate,
        String metaTitle,
        String metaDescription,
        boolean active,
        boolean featured,
        BigDecimal minPrice,
        int totalStock,
        boolean inStock,
        List<VariantResponse> variants,
        Long version,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}
