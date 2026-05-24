package org.ecommerce.project.service;

import org.ecommerce.project.model.Category;
import org.ecommerce.project.payload.CategoryDTO;
import org.ecommerce.project.payload.CategoryResponse;

import java.util.List;

public interface CategoryService {
    CategoryDTO updateCategory(CategoryDTO categoryDTO, Long categoryId);
    CategoryResponse getAllCategories(Integer pageNumber, Integer pageSize,String sortBy,String sortOrder);
    CategoryDTO createCategory(CategoryDTO categoryDTO);
    CategoryDTO deleteCategory(Long categoryId);

}
