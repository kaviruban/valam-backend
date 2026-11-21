package com.valam.ecommerce.product.mapper;

import com.valam.ecommerce.product.dto.CategoryResponse;
import com.valam.ecommerce.product.dto.ProductImageDto;
import com.valam.ecommerce.product.dto.ProductResponse;
import com.valam.ecommerce.product.dto.VariantResponse;
import com.valam.ecommerce.product.model.Category;
import com.valam.ecommerce.product.model.MeasureType;
import com.valam.ecommerce.product.model.Product;
import com.valam.ecommerce.product.model.ProductVariant;
import com.valam.ecommerce.product.model.QuantityUnit;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;

/** Entity -> DTO conversion. Keeps JPA entities out of the API contract. */
@Component
public class ProductMapper {

    private static final BigDecimal HUNDRED = BigDecimal.valueOf(100);

    public ProductResponse toResponse(Product product) {
        Category category = product.getCategory();
        MeasureType measureType = category.getMeasureType();
        List<VariantResponse> variants = product.getVariants().stream()
                .sorted(Comparator.comparing(ProductVariant::getQuantity))
                .map(v -> toResponse(v, measureType))
                .toList();
        List<ProductImageDto> images = product.getImages().stream()
                .map(i -> new ProductImageDto(i.getUrl(), i.getAltText()))
                .toList();

        BigDecimal minPrice = variants.stream().map(VariantResponse::price).min(Comparator.naturalOrder()).orElse(null);
        int totalStock = variants.stream().mapToInt(VariantResponse::stockQuantity).sum();

        return new ProductResponse(
                product.getId(),
                product.getProductCode(),
                product.getName(),
                product.getSlug(),
                product.getShortDescription(),
                product.getDescription(),
                category.getId(),
                category.getName(),
                measureType,
                images,
                images.isEmpty() ? null : images.getFirst().url(),
                List.copyOf(product.getHighlights()),
                product.getIngredients(),
                product.getUsageInstructions(),
                product.getStorageInstructions(),
                product.getShelfLife(),
                product.getCountryOfOrigin(),
                product.getHsnCode(),
                product.getGstRate(),
                product.getMetaTitle(),
                product.getMetaDescription(),
                product.isActive(),
                product.isFeatured(),
                minPrice,
                totalStock,
                totalStock > 0,
                variants,
                product.getVersion(),
                product.getCreatedAt(),
                product.getUpdatedAt()
        );
    }

    public VariantResponse toResponse(ProductVariant variant, MeasureType measureType) {
        return new VariantResponse(
                variant.getId(),
                variant.getIsbn(),
                variant.getQuantity(),
                measureType.getBaseUnit(),
                QuantityUnit.label(variant.getQuantity(), measureType),
                variant.getPrice(),
                variant.getMrp(),
                discountPercent(variant.getPrice(), variant.getMrp()),
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

    /** Rounded down so the storefront never advertises more saving than the customer gets. */
    static Integer discountPercent(BigDecimal price, BigDecimal mrp) {
        if (mrp == null || mrp.compareTo(price) <= 0) {
            return null;
        }
        int percent = mrp.subtract(price).multiply(HUNDRED).divide(mrp, 0, RoundingMode.FLOOR).intValue();
        return percent > 0 ? percent : null;
    }
}
