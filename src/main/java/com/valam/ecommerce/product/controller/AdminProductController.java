package com.valam.ecommerce.product.controller;

import com.valam.ecommerce.common.dto.PageResponse;
import com.valam.ecommerce.product.dto.ProductRequest;
import com.valam.ecommerce.product.dto.ProductResponse;
import com.valam.ecommerce.product.dto.StatusUpdateRequest;
import com.valam.ecommerce.product.service.ProductFilter;
import com.valam.ecommerce.product.service.ProductService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;

/** Seller / admin-panel product management. TODO: secure with ADMIN role once auth is added. */
@RestController
@RequestMapping("/api/v1/admin/products")
public class AdminProductController {

    private final ProductService productService;

    public AdminProductController(ProductService productService) {
        this.productService = productService;
    }

    /**
     * GET /api/v1/admin/products?categoryId=2&active=true&featured=false&inStock=false&q=rice
     * &page=0&size=20&sort=name,asc — sortable by name, category.name, createdAt, updatedAt.
     */
    @GetMapping
    public PageResponse<ProductResponse> list(
            @RequestParam(required = false) Long categoryId,
            @RequestParam(required = false) Boolean active,
            @RequestParam(required = false) Boolean featured,
            @RequestParam(required = false) Boolean inStock,
            @RequestParam(required = false) String q,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        return productService.search(new ProductFilter(categoryId, active, featured, inStock, q), pageable);
    }

    @GetMapping("/{id}")
    public ProductResponse get(@PathVariable Long id) {
        return productService.getById(id);
    }

    @PostMapping
    public ResponseEntity<ProductResponse> create(@Valid @RequestBody ProductRequest request) {
        ProductResponse created = productService.create(request);
        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}").buildAndExpand(created.id()).toUri();
        return ResponseEntity.created(location).body(created);
    }

    @PutMapping("/{id}")
    public ProductResponse update(@PathVariable Long id, @Valid @RequestBody ProductRequest request) {
        return productService.update(id, request);
    }

    /** Show / hide a product in the store without deleting it. */
    @PatchMapping("/{id}/status")
    public ProductResponse updateStatus(@PathVariable Long id, @Valid @RequestBody StatusUpdateRequest request) {
        return productService.updateStatus(id, request.active());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        productService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
