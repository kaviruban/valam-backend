package com.valam.ecommerce.product.dto;

import com.valam.ecommerce.product.model.MeasureType;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * Payload for creating a category and for fully replacing one (PUT).
 *
 * @param measureType WEIGHT (g/kg) or VOLUME (ml/L); can only change while the category has no products
 * @param sortOrder   position in the dropdown, lower first; defaults to 0 when omitted
 * @param active      defaults to true when omitted; inactive categories are hidden from the dropdown
 */
public record CategoryRequest(
        @NotBlank @Size(max = 100)
        String name,

        @NotNull
        MeasureType measureType,

        @Min(0)
        Integer sortOrder,

        Boolean active
) {
}
