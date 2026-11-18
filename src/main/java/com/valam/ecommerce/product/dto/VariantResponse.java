package com.valam.ecommerce.product.dto;

import com.valam.ecommerce.product.model.QuantityUnit;

import java.math.BigDecimal;

/**
 * @param quantity        quantity in {@code unit}, which is always the base unit (G or ML)
 * @param label           display text such as "500 g" or "1.5 kg"
 * @param mrp             maximum retail price, or null when not set
 * @param discountPercent whole-number saving versus MRP (e.g. 15), or null when there is no discount
 */
public record VariantResponse(
        Long id,
        String sku,
        Integer quantity,
        QuantityUnit unit,
        String label,
        BigDecimal price,
        BigDecimal mrp,
        Integer discountPercent,
        Integer stockQuantity,
        boolean inStock
) {
}
