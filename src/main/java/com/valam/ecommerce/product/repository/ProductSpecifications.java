package com.valam.ecommerce.product.repository;

import com.valam.ecommerce.product.model.Product;
import com.valam.ecommerce.product.model.ProductVariant;
import jakarta.persistence.criteria.Root;
import jakarta.persistence.criteria.Subquery;
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

    public static Specification<Product> isFeatured(boolean featured) {
        return (root, query, cb) -> cb.equal(root.get("featured"), featured);
    }

    /** In stock = at least one variant with stock left. */
    public static Specification<Product> inStock(boolean inStock) {
        return (root, query, cb) -> {
            Subquery<Long> sub = query.subquery(Long.class);
            Root<ProductVariant> v = sub.from(ProductVariant.class);
            sub.select(v.get("id")).where(
                    cb.equal(v.get("product"), root),
                    cb.greaterThan(v.get("stockQuantity"), 0));
            return inStock ? cb.exists(sub) : cb.not(cb.exists(sub));
        };
    }

    /**
     * Case-insensitive "contains" match on the product name, product code or any variant ISBN.
     * LIKE wildcards in the input are matched literally.
     */
    public static Specification<Product> matchesText(String text) {
        String pattern = "%" + text.toLowerCase(Locale.ROOT)
                .replace("\\", "\\\\")
                .replace("%", "\\%")
                .replace("_", "\\_") + "%";
        return (root, query, cb) -> {
            Subquery<Long> sub = query.subquery(Long.class);
            Root<ProductVariant> v = sub.from(ProductVariant.class);
            sub.select(v.get("id")).where(
                    cb.equal(v.get("product"), root),
                    cb.like(v.get("isbn"), pattern, '\\'));
            return cb.or(
                    cb.like(cb.lower(root.get("name")), pattern, '\\'),
                    cb.like(cb.lower(root.get("productCode")), pattern, '\\'),
                    cb.exists(sub));
        };
    }
}
