package com.valam.ecommerce.product.service;

import com.valam.ecommerce.common.exception.BusinessValidationException;
import com.valam.ecommerce.common.exception.ResourceNotFoundException;
import com.valam.ecommerce.product.dto.CategoryRequest;
import com.valam.ecommerce.product.dto.CategoryResponse;
import com.valam.ecommerce.product.mapper.ProductMapper;
import com.valam.ecommerce.product.model.Category;
import com.valam.ecommerce.product.repository.CategoryRepository;
import com.valam.ecommerce.product.repository.ProductRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional(readOnly = true)
public class CategoryService {

    private static final String RESOURCE = "Category";

    private final CategoryRepository categoryRepository;
    private final ProductRepository productRepository;
    private final ProductMapper productMapper;

    public CategoryService(CategoryRepository categoryRepository, ProductRepository productRepository,
                           ProductMapper productMapper) {
        this.categoryRepository = categoryRepository;
        this.productRepository = productRepository;
        this.productMapper = productMapper;
    }

    // ---------- Queries ----------

    /** Active categories only, in dropdown order. */
    public List<CategoryResponse> listActive() {
        return categoryRepository.findByActiveTrueOrderBySortOrderAscNameAsc().stream()
                .map(productMapper::toResponse).toList();
    }

    /** Every category, including inactive ones, for the admin's category management screen. */
    public List<CategoryResponse> listAll() {
        return categoryRepository.findAllByOrderBySortOrderAscNameAsc().stream()
                .map(productMapper::toResponse).toList();
    }

    public CategoryResponse getById(Long id) {
        return productMapper.toResponse(findCategory(id));
    }

    // ---------- Commands ----------

    @Transactional
    public CategoryResponse create(CategoryRequest request) {
        String name = request.name().trim();
        if (categoryRepository.existsByNameIgnoreCase(name)) {
            throw duplicateName(name);
        }
        Category category = new Category();
        category.setMeasureType(request.measureType());
        applyDetails(category, name, request);
        return productMapper.toResponse(categoryRepository.save(category));
    }

    @Transactional
    public CategoryResponse update(Long id, CategoryRequest request) {
        Category category = findCategory(id);
        String name = request.name().trim();
        if (categoryRepository.existsByNameIgnoreCaseAndIdNot(name, id)) {
            throw duplicateName(name);
        }
        // Existing pack sizes are stored in this category's base unit (g or ml); switching would corrupt them.
        if (request.measureType() != category.getMeasureType() && productRepository.existsByCategoryId(id)) {
            throw new BusinessValidationException(
                    "Cannot change the measure type of category '" + category.getName()
                            + "' because it already has products.");
        }
        category.setMeasureType(request.measureType());
        applyDetails(category, name, request);
        return productMapper.toResponse(categoryRepository.saveAndFlush(category));
    }

    /** Only empty categories can be deleted; deactivate a category to hide one that has products. */
    @Transactional
    public void delete(Long id) {
        Category category = findCategory(id);
        if (productRepository.existsByCategoryId(id)) {
            throw new BusinessValidationException(
                    "Category '" + category.getName() + "' has products. Move or delete them first, "
                            + "or set the category inactive to hide it.");
        }
        categoryRepository.delete(category);
    }

    /** For product create/update. A product may keep an inactive category it already has, but not be moved into one. */
    Category findAssignable(Long id, Category current) {
        Category category = findCategory(id);
        if (!category.isActive() && (current == null || !current.getId().equals(id))) {
            throw new BusinessValidationException("Category '" + category.getName() + "' is inactive.");
        }
        return category;
    }

    // ---------- Internals ----------

    private Category findCategory(Long id) {
        return categoryRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException(RESOURCE, id));
    }

    private static void applyDetails(Category category, String name, CategoryRequest request) {
        category.setName(name);
        category.setSortOrder(request.sortOrder() == null ? 0 : request.sortOrder());
        category.setActive(request.active() == null || request.active());
    }

    private static BusinessValidationException duplicateName(String name) {
        return new BusinessValidationException("A category named '" + name + "' already exists.");
    }
}
