package org.ecommerce.project.service;

import org.ecommerce.project.expectionsHandler.APIException;
import org.ecommerce.project.expectionsHandler.NoCategoryPresentExceptio;
import org.ecommerce.project.expectionsHandler.ResourceNotFoundException;
import org.ecommerce.project.model.Category;
import org.ecommerce.project.repository.CategoryRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
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
        List<Category> categoriesNotFound = categoryRepository.findAll();
        if (categoriesNotFound.isEmpty()) {
            throw new NoCategoryPresentExceptio("No categories found");
        }
        return categoriesNotFound;
    }


    @Override
    public void createCategory(Category category) {
        Category alreadyExistCategory = categoryRepository.findByCategoryName(category.getCategoryName());
        if (alreadyExistCategory != null){
            throw  new APIException("Category with name "+ category.getCategoryName() + "already exist");
        }
        categoryRepository.save(category);
    }



    @Override
    public String deleteCategory(Long categoryId) {
        Optional<Category> categories = categoryRepository.findById(categoryId);

        Category detectCategory = categories.
                orElseThrow(() -> new ResourceNotFoundException("Category","CategoryId",categoryId));

        categoryRepository.delete(detectCategory);
        return "Category with categoryId: " + categoryId + " deleted successfully !!";
    }



    @Override
    public Category updateCategory(Category category, Long categoryId) {
        Optional<Category> savedCategoriesOptional = categoryRepository.findById(categoryId);

        Category savedCategory = savedCategoriesOptional
                .orElseThrow(() -> new ResourceNotFoundException("Category","CategoryId",categoryId));

        category.setCategoryId(categoryId);
        savedCategory=categoryRepository.save(category);
        return savedCategory;
    }
}
