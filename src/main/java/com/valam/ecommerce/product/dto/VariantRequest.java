package com.valam.ecommerce.product.dto;

import com.valam.ecommerce.product.model.QuantityUnit;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

/**
 * A pack size as entered by the seller, e.g. {quantity: 500, unit: G} or {quantity: 1, unit: KG}.
 *
 * @param id omit to add a new variant; set it to update an existing variant (update requests only)
 */
public record VariantRequest(
        Long id,

        @NotNull @DecimalMin(value = "0", inclusive = false)
        BigDecimal quantity,

        @NotNull
        QuantityUnit unit,

        @NotNull @DecimalMin("0.00") @Digits(integer = 10, fraction = 2)
        BigDecimal price,

        @NotNull @Min(0)
        Integer stockQuantity
) {
}
