package com.valam.ecommerce.product.repository;

import com.valam.ecommerce.product.model.Product;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.Optional;

public interface ProductRepository extends JpaRepository<Product, Long>, JpaSpecificationExecutor<Product> {

    /** Loads a product together with its category and variants; images and highlights load in batches. */
    @EntityGraph(attributePaths = {"category", "variants"})
    Optional<Product> findWithVariantsById(Long id);

    boolean existsByCategoryId(Long categoryId);

    boolean existsBySlug(String slug);

    boolean existsBySlugAndIdNot(String slug, Long id);
}
