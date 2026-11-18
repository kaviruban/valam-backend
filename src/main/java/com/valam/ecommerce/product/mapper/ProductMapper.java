package com.valam.ecommerce.product.mapper;

import com.valam.ecommerce.product.dto.CategoryResponse;
import com.valam.ecommerce.product.dto.ProductResponse;
import com.valam.ecommerce.product.dto.VariantResponse;
import com.valam.ecommerce.product.model.Category;
import com.valam.ecommerce.product.model.MeasureType;
import com.valam.ecommerce.product.model.Product;
import com.valam.ecommerce.product.model.ProductVariant;
import com.valam.ecommerce.product.model.QuantityUnit;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.Comparator;
import java.util.List;

/** Entity -> DTO conversion. Keeps JPA entities out of the API contract. */
@Component
public class ProductMapper {

    public ProductResponse toResponse(Product product) {
        MeasureType measureType = product.getCategory().getMeasureType();
        List<VariantResponse> variants = product.getVariants().stream()
                .sorted(Comparator.comparing(ProductVariant::getQuantity))
                .map(v -> toResponse(v, measureType))
                .toList();

        return new ProductResponse(
                product.getId(),
                product.getName(),
                product.getDescription(),
                product.getCategory().getId(),
                product.getCategory().getName(),
                product.getImageUrl(),
                product.isActive(),
                variants,
                product.getCreatedAt(),
                product.getUpdatedAt()
        );
    }

    public VariantResponse toResponse(ProductVariant variant, MeasureType measureType) {
        return new VariantResponse(
                variant.getId(),
                variant.getQuantity(),
                measureType.getBaseUnit(),
                QuantityUnit.label(variant.getQuantity(), measureType),
                variant.getPrice(),
                variant.getStockQuantity(),
                variant.getStockQuantity() > 0
        );
    }

    public CategoryResponse toResponse(Category category) {
        List<QuantityUnit> units = Arrays.stream(QuantityUnit.values())
                .filter(u -> u.getMeasureType() == category.getMeasureType())
                .toList();
        return new CategoryResponse(category.getId(), category.getName(), category.getMeasureType(), units,
                category.isActive(), category.getSortOrder());
    }
}
