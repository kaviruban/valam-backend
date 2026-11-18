package com.valam.ecommerce.product.dto;

import com.valam.ecommerce.product.model.MeasureType;
import com.valam.ecommerce.product.model.QuantityUnit;

import java.util.List;

/** Lets the admin UI build its category dropdown (id + name) and the matching unit options. */
public record CategoryResponse(
        Long id,
        String name,
        MeasureType measureType,
        List<QuantityUnit> allowedUnits,
        boolean active,
        int sortOrder
) {
}
