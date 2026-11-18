package com.valam.ecommerce.product.repository;

import com.valam.ecommerce.product.model.ProductVariant;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ProductVariantRepository extends JpaRepository<ProductVariant, Long> {

    Optional<ProductVariant> findBySkuIgnoreCase(String sku);
}
