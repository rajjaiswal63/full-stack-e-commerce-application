package org.ecommerce.project.service;

import org.ecommerce.project.model.Category;

import java.util.List;

public interface CategoryService {
    Category updateCategory(Category category, Long categoryId);
    List<Category> getAllCategories();
    void createCategory(Category category);
    String deleteCategory(Long categoryId);

}
