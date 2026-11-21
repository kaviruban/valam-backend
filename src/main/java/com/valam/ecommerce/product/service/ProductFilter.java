package com.valam.ecommerce.product.service;

/**
 * Optional filters for the admin product list; null means "don't filter on this".
 *
 * @param q text matched against the product name, product code or a variant ISBN
 */
public record ProductFilter(Long categoryId, Boolean active, Boolean featured, Boolean inStock, String q) {
}
