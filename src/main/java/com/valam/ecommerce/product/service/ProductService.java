package com.valam.ecommerce.product.service;

import com.valam.ecommerce.common.dto.PageResponse;
import com.valam.ecommerce.common.exception.BusinessValidationException;
import com.valam.ecommerce.common.exception.ResourceNotFoundException;
import com.valam.ecommerce.product.dto.ProductRequest;
import com.valam.ecommerce.product.dto.ProductResponse;
import com.valam.ecommerce.product.dto.VariantRequest;
import com.valam.ecommerce.product.mapper.ProductMapper;
import com.valam.ecommerce.product.model.Category;
import com.valam.ecommerce.product.model.Product;
import com.valam.ecommerce.product.model.ProductVariant;
import com.valam.ecommerce.product.repository.ProductRepository;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import static com.valam.ecommerce.product.repository.ProductSpecifications.hasCategory;
import static com.valam.ecommerce.product.repository.ProductSpecifications.isActive;
import static com.valam.ecommerce.product.repository.ProductSpecifications.nameContains;

@Service
@Transactional(readOnly = true)
public class ProductService {

    private static final String RESOURCE = "Product";
    private static final Set<String> SORTABLE_FIELDS = Set.of("name", "category.name", "createdAt", "updatedAt");

    private final ProductRepository productRepository;
    private final CategoryService categoryService;
    private final ProductMapper productMapper;

    public ProductService(ProductRepository productRepository, CategoryService categoryService,
                          ProductMapper productMapper) {
        this.productRepository = productRepository;
        this.categoryService = categoryService;
        this.productMapper = productMapper;
    }

    // ---------- Queries ----------

    /** Paged search; every filter is optional. */
    public PageResponse<ProductResponse> search(Long categoryId, Boolean active, String q, Pageable pageable) {
        validateSort(pageable.getSort());

        List<Specification<Product>> filters = new ArrayList<>();
        if (categoryId != null) filters.add(hasCategory(categoryId));
        if (active != null) filters.add(isActive(active));
        if (StringUtils.hasText(q)) filters.add(nameContains(q.trim()));

        return PageResponse.from(
                productRepository.findAll(Specification.allOf(filters), pageable).map(productMapper::toResponse));
    }

    public ProductResponse getById(Long id) {
        return productMapper.toResponse(findProduct(id));
    }

    // ---------- Commands ----------

    @Transactional
    public ProductResponse create(ProductRequest request) {
        Product product = new Product();
        applyDetails(product, request);

        for (VariantRequest vr : request.variants()) {
            if (vr.id() != null) {
                throw new BusinessValidationException("Variant id must not be set when creating a product.");
            }
        }
        reconcileVariants(product, request);

        return productMapper.toResponse(productRepository.save(product));
    }

    /** Full replacement: variants missing from the request are removed, ones with an id are updated. */
    @Transactional
    public ProductResponse update(Long id, ProductRequest request) {
        Product product = findProduct(id);
        applyDetails(product, request);
        reconcileVariants(product, request);
        return productMapper.toResponse(productRepository.saveAndFlush(product));
    }

    @Transactional
    public ProductResponse updateStatus(Long id, boolean active) {
        Product product = findProduct(id);
        product.setActive(active);
        return productMapper.toResponse(productRepository.saveAndFlush(product));
    }

    @Transactional
    public void delete(Long id) {
        Product product = findProduct(id);
        productRepository.delete(product);
    }

    // ---------- Internals ----------

    private Product findProduct(Long id) {
        return productRepository.findWithVariantsById(id)
                .orElseThrow(() -> new ResourceNotFoundException(RESOURCE, id));
    }

    private void applyDetails(Product product, ProductRequest request) {
        product.setName(request.name().trim());
        product.setDescription(request.description());
        product.setCategory(categoryService.findAssignable(request.categoryId(), product.getCategory()));
        product.setImageUrl(StringUtils.hasText(request.imageUrl()) ? request.imageUrl().trim() : null);
        product.setActive(request.active() == null || request.active());
    }

    /** Makes the product's variants match the request: update by id, add new, drop the rest. */
    private static void reconcileVariants(Product product, ProductRequest request) {
        Category category = product.getCategory();
        Map<Long, ProductVariant> existing = product.getVariants().stream()
                .collect(Collectors.toMap(ProductVariant::getId, Function.identity()));

        Set<Integer> seenQuantities = new HashSet<>();
        Set<Long> keptIds = new HashSet<>();
        List<ProductVariant> toAdd = new ArrayList<>();

        for (VariantRequest vr : request.variants()) {
            int baseQuantity = toBaseQuantity(vr, category);
            if (!seenQuantities.add(baseQuantity)) {
                throw new BusinessValidationException(
                        "Duplicate pack size: " + vr.quantity().toPlainString() + " " + vr.unit().getSymbol());
            }

            ProductVariant variant;
            if (vr.id() == null) {
                variant = new ProductVariant();
                toAdd.add(variant);
            } else {
                variant = existing.get(vr.id());
                if (variant == null) {
                    throw new BusinessValidationException(
                            "Variant " + vr.id() + " does not belong to product " + product.getId());
                }
                if (!keptIds.add(vr.id())) {
                    throw new BusinessValidationException("Variant " + vr.id() + " is listed more than once.");
                }
            }
            variant.setQuantity(baseQuantity);
            variant.setPrice(vr.price());
            variant.setStockQuantity(vr.stockQuantity());
        }

        existing.values().stream()
                .filter(v -> !keptIds.contains(v.getId()))
                .toList()
                .forEach(product::removeVariant);
        toAdd.forEach(product::addVariant);
    }

    /** Validates the unit against the category and converts to grams / millilitres. */
    private static int toBaseQuantity(VariantRequest vr, Category category) {
        if (vr.unit().getMeasureType() != category.getMeasureType()) {
            throw new BusinessValidationException(
                    "Unit " + vr.unit() + " is not allowed for category '" + category.getName()
                            + "' (measured by " + category.getMeasureType().name().toLowerCase() + ").");
        }
        try {
            return vr.unit().toBase(vr.quantity());
        } catch (ArithmeticException e) {
            throw new BusinessValidationException(
                    "Pack size " + vr.quantity().toPlainString() + " " + vr.unit().getSymbol()
                            + " must be a whole number of " + category.getMeasureType().getBaseUnit().getSymbol()
                            + " and within range.");
        }
    }

    private static void validateSort(Sort sort) {
        for (Sort.Order order : sort) {
            if (!SORTABLE_FIELDS.contains(order.getProperty())) {
                throw new BusinessValidationException(
                        "Cannot sort by '" + order.getProperty() + "'. Allowed: " + SORTABLE_FIELDS);
            }
        }
    }
}
