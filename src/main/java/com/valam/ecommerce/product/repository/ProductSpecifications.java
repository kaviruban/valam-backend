package com.valam.ecommerce.product.repository;

import com.valam.ecommerce.product.model.Product;
import org.springframework.data.jpa.domain.Specification;

import java.util.Locale;

/** Composable query filters for products. Null-safe callers decide which to apply. */
public final class ProductSpecifications {

    private ProductSpecifications() {
    }

    public static Specification<Product> hasCategory(Long categoryId) {
        return (root, query, cb) -> cb.equal(root.get("category").get("id"), categoryId);
    }

    public static Specification<Product> isActive(boolean active) {
        return (root, query, cb) -> cb.equal(root.get("active"), active);
    }

    /** Case-insensitive "contains" match on name; LIKE wildcards in the input are matched literally. */
    public static Specification<Product> nameContains(String text) {
        String escaped = text.toLowerCase(Locale.ROOT)
                .replace("\\", "\\\\")
                .replace("%", "\\%")
                .replace("_", "\\_");
        return (root, query, cb) -> cb.like(cb.lower(root.get("name")), "%" + escaped + "%", '\\');
    }
}
