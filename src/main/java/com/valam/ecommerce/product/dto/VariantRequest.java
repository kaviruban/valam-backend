package com.valam.ecommerce.product.dto;

import com.valam.ecommerce.product.model.QuantityUnit;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

/**
 * A pack size as entered by the seller, e.g. {quantity: 500, unit: G} or {quantity: 1, unit: KG}.
 *
 * @param id    omit to add a new variant; set it to update an existing variant (update requests only)
 * @param sku   optional stock keeping unit, unique across the store (case-insensitive, stored upper-case)
 * @param price selling price, GST-inclusive
 * @param mrp   optional maximum retail price; must be at least {@code price}
 */
public record VariantRequest(
        Long id,

        @Size(max = 64)
        @Pattern(regexp = "^[A-Za-z0-9][A-Za-z0-9_-]*$", message = "may contain only letters, digits, '-' and '_'")
        String sku,

        @NotNull @DecimalMin(value = "0", inclusive = false)
        BigDecimal quantity,

        @NotNull
        QuantityUnit unit,

        @NotNull @DecimalMin("0.00") @Digits(integer = 10, fraction = 2)
        BigDecimal price,

        @DecimalMin(value = "0", inclusive = false) @Digits(integer = 10, fraction = 2)
        BigDecimal mrp,

        @NotNull @Min(0)
        Integer stockQuantity
) {
}
