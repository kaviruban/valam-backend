package com.valam.ecommerce.product.repository;

import com.valam.ecommerce.product.model.ProductVariant;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProductVariantRepository extends JpaRepository<ProductVariant, Long> {
}
