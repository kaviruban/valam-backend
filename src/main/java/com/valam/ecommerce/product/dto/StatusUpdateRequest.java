package com.valam.ecommerce.product.dto;

import jakarta.validation.constraints.NotNull;

public record StatusUpdateRequest(@NotNull Boolean active) {
}
