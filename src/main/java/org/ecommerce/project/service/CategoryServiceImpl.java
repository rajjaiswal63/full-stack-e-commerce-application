package org.ecommerce.project.service;

import org.ecommerce.project.exceptions.APIException;
import org.ecommerce.project.exceptions.ResourceNotFoundException;
import org.ecommerce.project.model.Category;
import org.ecommerce.project.payload.CategoryDTO;
import org.ecommerce.project.payload.CategoryResponse;
import org.ecommerce.project.repository.CategoryRepository;
import org.ecommerce.project.repository.ProductRepository;
import org.modelmapper.ModelMapper;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Set;

@Service
@Transactional
public class CategoryServiceImpl implements CategoryService {
    private final CategoryRepository categoryRepository;
    private final ProductRepository productRepository;
    private final ModelMapper modelMapper;
    private static final Set<String> ALLOWED_SORTS = Set.of("categoryId", "categoryName");

    public CategoryServiceImpl(CategoryRepository categoryRepository, ProductRepository productRepository,
                               ModelMapper modelMapper) {
        this.categoryRepository = categoryRepository;
        this.productRepository = productRepository;
        this.modelMapper = modelMapper;
    }

    @Override
    @Transactional(readOnly = true)
    public CategoryResponse getAllCategories(Integer pageNumber, Integer pageSize, String sortBy, String sortOrder) {
        validatePaging(pageNumber, pageSize);
        String sortField = sortBy != null && ALLOWED_SORTS.contains(sortBy) ? sortBy : "categoryId";
        Sort sort = "asc".equalsIgnoreCase(sortOrder) ? Sort.by(sortField).ascending() : Sort.by(sortField).descending();
        Page<Category> page = categoryRepository.findAll(PageRequest.of(pageNumber, pageSize, sort));
        List<CategoryDTO> content = page.getContent().stream()
                .map(category -> modelMapper.map(category, CategoryDTO.class)).toList();

        CategoryResponse response = new CategoryResponse();
        response.setContent(content);
        response.setPageNumber(page.getNumber());
        response.setPageSize(page.getSize());
        response.setTotalElements(page.getTotalElements());
        response.setTotalPages(page.getTotalPages());
        response.setLastPage(page.isLast());
        return response;
    }

    @Override
    public CategoryDTO createCategory(CategoryDTO categoryDTO) {
        String name = categoryDTO.getCategoryName().trim();
        if (categoryRepository.findByCategoryName(name) != null) {
            throw new APIException("Category with the name " + name + " already exists.");
        }
        Category category = new Category();
        category.setCategoryName(name);
        return modelMapper.map(categoryRepository.save(category), CategoryDTO.class);
    }

    @Override
    public CategoryDTO deleteCategory(Long categoryId) {
        Category category = findCategory(categoryId);
        if (productRepository.existsByCategoryCategoryId(categoryId)) {
            throw new APIException("Move or remove the category's products before deleting this category.");
        }
        categoryRepository.delete(category);
        return modelMapper.map(category, CategoryDTO.class);
    }

    @Override
    public CategoryDTO updateCategory(CategoryDTO categoryDTO, Long categoryId) {
        Category category = findCategory(categoryId);
        String name = categoryDTO.getCategoryName().trim();
        Category duplicate = categoryRepository.findByCategoryName(name);
        if (duplicate != null && !duplicate.getCategoryId().equals(categoryId)) {
            throw new APIException("Category with the name " + name + " already exists.");
        }
        category.setCategoryName(name);
        return modelMapper.map(categoryRepository.save(category), CategoryDTO.class);
    }

    private Category findCategory(Long categoryId) {
        return categoryRepository.findById(categoryId)
                .orElseThrow(() -> new ResourceNotFoundException("Category", "categoryId", categoryId));
    }

    private void validatePaging(Integer pageNumber, Integer pageSize) {
        if (pageNumber == null || pageNumber < 0 || pageSize == null || pageSize < 1 || pageSize > 100) {
            throw new APIException("pageNumber must be non-negative and pageSize must be between 1 and 100.");
        }
    }
}
