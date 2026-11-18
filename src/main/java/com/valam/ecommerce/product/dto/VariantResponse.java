package com.valam.ecommerce.product.dto;

import com.valam.ecommerce.product.model.QuantityUnit;

import java.math.BigDecimal;

/**
 * @param quantity quantity in {@code unit}, which is always the base unit (G or ML)
 * @param label    display text such as "500 g" or "1.5 kg"
 */
public record VariantResponse(
        Long id,
        Integer quantity,
        QuantityUnit unit,
        String label,
        BigDecimal price,
        Integer stockQuantity,
        boolean inStock
) {
}
