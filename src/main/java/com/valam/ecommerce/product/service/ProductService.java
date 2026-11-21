package com.valam.ecommerce.product.service;

import com.valam.ecommerce.common.dto.PageResponse;
import com.valam.ecommerce.common.exception.BusinessValidationException;
import com.valam.ecommerce.common.exception.ConflictException;
import com.valam.ecommerce.common.exception.ResourceNotFoundException;
import com.valam.ecommerce.common.util.Slugs;
import com.valam.ecommerce.product.dto.ProductRequest;
import com.valam.ecommerce.product.dto.ProductResponse;
import com.valam.ecommerce.product.dto.VariantRequest;
import com.valam.ecommerce.product.mapper.ProductMapper;
import com.valam.ecommerce.product.model.Category;
import com.valam.ecommerce.product.model.Product;
import com.valam.ecommerce.product.model.ProductImage;
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
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import static com.valam.ecommerce.product.repository.ProductSpecifications.hasCategory;
import static com.valam.ecommerce.product.repository.ProductSpecifications.inStock;
import static com.valam.ecommerce.product.repository.ProductSpecifications.isActive;
import static com.valam.ecommerce.product.repository.ProductSpecifications.isFeatured;
import static com.valam.ecommerce.product.repository.ProductSpecifications.matchesText;

@Service
@Transactional(readOnly = true)
public class ProductService {

    private static final String RESOURCE = "Product";
    private static final String DEFAULT_COUNTRY = "India";
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

    /** Paged admin search; every filter is optional. {@code q} matches the product name, product code or a variant ISBN. */
    public PageResponse<ProductResponse> search(ProductFilter filter, Pageable pageable) {
        validateSort(pageable.getSort());

        List<Specification<Product>> specs = new ArrayList<>();
        if (filter.categoryId() != null) specs.add(hasCategory(filter.categoryId()));
        if (filter.active() != null) specs.add(isActive(filter.active()));
        if (filter.featured() != null) specs.add(isFeatured(filter.featured()));
        if (filter.inStock() != null) specs.add(inStock(filter.inStock()));
        if (StringUtils.hasText(filter.q())) specs.add(matchesText(filter.q().trim()));

        return PageResponse.from(
                productRepository.findAll(Specification.allOf(specs), pageable).map(productMapper::toResponse));
    }

    public ProductResponse getById(Long id) {
        return productMapper.toResponse(findProduct(id));
    }

    // ---------- Commands ----------

    @Transactional
    public ProductResponse create(ProductRequest request) {
        for (VariantRequest vr : request.variants()) {
            if (vr.id() != null) {
                throw new BusinessValidationException("Variant id must not be set when creating a product.");
            }
        }
        Product product = new Product();
        // All lookups run before the entity is touched, so no half-applied state is flushed by a query.
        Category category = categoryService.findAssignable(request.categoryId(), null);
        String slug = resolveSlug(request, null);
        List<VariantValues> variants = prepareVariants(request, category, product);

        product.setCategory(category);
        product.setSlug(slug);
        applyDetails(product, request);
        applyVariants(product, variants);

        return productMapper.toResponse(productRepository.save(product));
    }

    /**
     * Full replacement: optional fields left out are cleared, variants missing from the request are removed and
     * ones with an id are updated.
     */
    @Transactional
    public ProductResponse update(Long id, ProductRequest request) {
        Product product = findProduct(id);
        if (request.version() != null && !request.version().equals(product.getVersion())) {
            throw staleEdit();
        }
        Category category = categoryService.findAssignable(request.categoryId(), product.getCategory());
        String slug = resolveSlug(request, product);
        List<VariantValues> variants = prepareVariants(request, category, product);

        product.setCategory(category);
        product.setSlug(slug);
        applyDetails(product, request);
        applyVariants(product, variants);
        product.touch();

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

    private static ConflictException staleEdit() {
        return new ConflictException("This product was changed by someone else. Reload it and try again.");
    }

    /** An explicit slug must be free; otherwise keep the current one, or generate one from the name. */
    private String resolveSlug(ProductRequest request, Product existing) {
        if (request.slug() != null) {
            boolean taken = existing == null
                    ? productRepository.existsBySlug(request.slug())
                    : productRepository.existsBySlugAndIdNot(request.slug(), existing.getId());
            if (taken) {
                throw new BusinessValidationException("The URL slug '" + request.slug() + "' is already used.");
            }
            return request.slug();
        }
        if (existing != null) {
            return existing.getSlug(); // keep storefront links stable when the name changes
        }
        String base = Slugs.slugify(request.name());
        if (base.isEmpty()) {
            base = "product";
        }
        String candidate = base;
        for (int n = 2; productRepository.existsBySlug(candidate); n++) {
            candidate = base + "-" + n;
        }
        return candidate;
    }

    private static void applyDetails(Product product, ProductRequest request) {
        product.setName(request.name().trim());
        product.setShortDescription(trimToNull(request.shortDescription()));
        product.setDescription(trimToNull(request.description()));
        product.replaceImages(request.images() == null ? List.of() : request.images().stream()
                .map(i -> new ProductImage(i.url().trim(), trimToNull(i.altText())))
                .toList());
        product.replaceHighlights(request.highlights() == null ? List.of() : request.highlights().stream()
                .map(String::trim)
                .toList());
        product.setIngredients(trimToNull(request.ingredients()));
        product.setUsageInstructions(trimToNull(request.usageInstructions()));
        product.setStorageInstructions(trimToNull(request.storageInstructions()));
        product.setShelfLife(trimToNull(request.shelfLife()));
        product.setCountryOfOrigin(Objects.requireNonNullElse(trimToNull(request.countryOfOrigin()), DEFAULT_COUNTRY));
        product.setHsnCode(request.hsnCode());
        product.setGstRate(request.gstRate());
        product.setMetaTitle(trimToNull(request.metaTitle()));
        product.setMetaDescription(trimToNull(request.metaDescription()));
        product.setActive(request.active() == null || request.active());
        product.setFeatured(Boolean.TRUE.equals(request.featured()));
    }

    /** A validated variant row, ready to apply. {@code target} is null for a new variant. */
    private record VariantValues(ProductVariant target, VariantRequest request, int baseQuantity) {
    }

    /** Validates every variant (units, pack sizes, MRP) without changing anything. */
    private List<VariantValues> prepareVariants(ProductRequest request, Category category, Product product) {
        Map<Long, ProductVariant> existing = product.getVariants().stream()
                .collect(Collectors.toMap(ProductVariant::getId, Function.identity()));

        Set<Integer> seenQuantities = new HashSet<>();
        Set<Long> seenIds = new HashSet<>();
        List<VariantValues> result = new ArrayList<>();

        for (VariantRequest vr : request.variants()) {
            int baseQuantity = toBaseQuantity(vr, category);
            if (!seenQuantities.add(baseQuantity)) {
                throw new BusinessValidationException(
                        "Duplicate pack size: " + vr.quantity().toPlainString() + " " + vr.unit().getSymbol());
            }

            ProductVariant target = null;
            if (vr.id() != null) {
                target = existing.get(vr.id());
                if (target == null) {
                    throw new BusinessValidationException(
                            "Variant " + vr.id() + " does not belong to product " + product.getId());
                }
                if (!seenIds.add(vr.id())) {
                    throw new BusinessValidationException("Variant " + vr.id() + " is listed more than once.");
                }
            }

            if (vr.mrp() != null && vr.mrp().compareTo(vr.price()) < 0) {
                throw new BusinessValidationException(
                        "MRP " + vr.mrp().toPlainString() + " is below the selling price " + vr.price().toPlainString()
                                + " for pack size " + vr.quantity().toPlainString() + " " + vr.unit().getSymbol() + ".");
            }

            result.add(new VariantValues(target, vr, baseQuantity));
        }
        return result;
    }

    /** Makes the product's variants match the prepared list: update by id, add new, drop the rest. */
    private static void applyVariants(Product product, List<VariantValues> values) {
        Set<ProductVariant> kept = new HashSet<>();
        List<ProductVariant> toAdd = new ArrayList<>();
        for (VariantValues v : values) {
            ProductVariant variant = v.target();
            if (variant == null) {
                variant = new ProductVariant();
                toAdd.add(variant);
            } else {
                kept.add(variant);
            }
            variant.setQuantity(v.baseQuantity());
            variant.setPrice(v.request().price());
            variant.setMrp(v.request().mrp());
            variant.setStockQuantity(v.request().stockQuantity());
        }
        product.getVariants().stream()
                .filter(v -> !kept.contains(v))
                .toList()
                .forEach(product::removeVariant);
        toAdd.forEach(product::addVariant);
    }

    /** Validates the unit against the category and converts to grams / millilitres. */
    private static int toBaseQuantity(VariantRequest vr, Category category) {
        if (vr.unit().getMeasureType() != category.getMeasureType()) {
            throw new BusinessValidationException(
                    "Unit " + vr.unit() + " is not allowed for category '" + category.getName()
                            + "' (measured by " + category.getMeasureType().name().toLowerCase(Locale.ROOT) + ").");
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

    private static String trimToNull(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
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
