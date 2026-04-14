package org.ecommerce.project.service;

import org.ecommerce.project.model.Category;
import org.ecommerce.project.repository.CategoryRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
public class CategoryServiceImpl implements CategoryService {
    @Autowired
    private CategoryRepository categoryRepository;


    @Override
    public List<Category> getAllCategories() {
        return categoryRepository.findAll();
    }


    @Override
    public void createCategory(Category category) {
        categoryRepository.save(category);
    }



    @Override
    public String deleteCategory(Long categoryId) {
        Optional<Category> categories = categoryRepository.findById(categoryId);

        Category detectCategory = categories.
                orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,"Category not found"));

        categoryRepository.delete(detectCategory);
        return "Category with categoryId: " + categoryId + " deleted successfully !!";
    }



    @Override
    public Category updateCategory(Category category, Long categoryId) {
        Optional<Category> savedCategoriesOptional = categoryRepository.findById(categoryId);

        Category savedCategory = savedCategoriesOptional
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,"Category not found"));

        category.setCategoryId(categoryId);
        savedCategory=categoryRepository.save(category);
        return savedCategory;
    }
}
