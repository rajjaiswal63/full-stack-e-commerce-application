package org.ecommerce.project.service;

import org.ecommerce.project.expectionsHandler.APIException;
import org.ecommerce.project.expectionsHandler.NoCategoryPresentExceptio;
import org.ecommerce.project.expectionsHandler.ResourceNotFoundException;
import org.ecommerce.project.model.Category;
import org.ecommerce.project.payload.CategoryDTO;
import org.ecommerce.project.payload.CategoryResponse;
import org.ecommerce.project.repository.CategoryRepository;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.util.List;
import java.util.Optional;

@Service
public class CategoryServiceImpl implements CategoryService {
    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    private ModelMapper modelMapper;

    @Override
    public CategoryResponse getAllCategories() {
        List<Category> categories = categoryRepository.findAll();
        if (categories.isEmpty()) {
            throw new NoCategoryPresentExceptio("No categories found");
        }
        List<CategoryDTO> categoriesDTO = categories.stream()
                .map(category -> modelMapper.map(category, CategoryDTO.class))
                .toList();
        CategoryResponse categoryResponse = new CategoryResponse();
        categoryResponse.setCategoriesList(categoriesDTO);
        return categoryResponse;
    }

    @Override
    public CategoryDTO createCategory(CategoryDTO categoryDTO) {
        Category category = modelMapper.map(categoryDTO, Category.class);
        Category alreadyExistCategory = categoryRepository.findByCategoryName(category.getCategoryName());
        if (alreadyExistCategory != null){
            throw  new APIException("Category with name "+ category.getCategoryName() + "already exist");
        }
        Category categoryToReturn =categoryRepository.save(category);
        return modelMapper.map(categoryToReturn, CategoryDTO.class);
    }

    @Override
    public CategoryDTO deleteCategory(Long categoryId) {
        Optional<Category> categories = categoryRepository.findById(categoryId);
        Category detectCategory = categories.
                orElseThrow(() -> new ResourceNotFoundException("Category","CategoryId",categoryId));

        CategoryDTO categoryToReturn = modelMapper.map(detectCategory, CategoryDTO.class);
        categoryRepository.delete(detectCategory);
        return categoryToReturn;
    }

    @Override
    public CategoryDTO updateCategory(CategoryDTO categoryDTO, Long categoryId) {
        Category category = modelMapper.map(categoryDTO, Category.class);
        Optional<Category> savedCategoriesOptional = categoryRepository.findById(categoryId);

        Category savedCategory = savedCategoriesOptional
                .orElseThrow(() -> new ResourceNotFoundException("Category","CategoryId",categoryId));

        category.setCategoryId(categoryId);
        Category categoryToReturn=savedCategory=categoryRepository.save(category);
        return modelMapper.map(categoryToReturn, CategoryDTO.class);
    }
}
