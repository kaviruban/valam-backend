package com.valam.ecommerce.product.dto;

import java.time.LocalDateTime;
import java.util.List;

public record ProductResponse(
        Long id,
        String name,
        String description,
        Long categoryId,
        String categoryName,
        String imageUrl,
        boolean active,
        List<VariantResponse> variants,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}
