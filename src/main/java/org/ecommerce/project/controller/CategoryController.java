package org.ecommerce.project.controller;

import jakarta.validation.Valid;
import org.ecommerce.project.config.AppConstant;
import org.ecommerce.project.payload.CategoryDTO;
import org.ecommerce.project.payload.CategoryResponse;
import org.ecommerce.project.service.CategoryService;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api")
public class CategoryController {
    @Autowired
    private CategoryService categoryService;

    @GetMapping("/echo")
    public ResponseEntity<String> echo(@RequestParam(name = "message", required = false) String message){
        return new ResponseEntity<>("message : " + message, HttpStatus.OK);
    }

    @GetMapping("/public/categories")
    public ResponseEntity<CategoryResponse> getCategories(
            @RequestParam(name="pageNumber" , defaultValue = AppConstant.pageNumber, required = false) Integer pageNumber,
            @RequestParam(name="pageSize",  defaultValue = AppConstant.pageSize, required = false) Integer pageSize,
            @RequestParam(name = "sortBy", defaultValue = AppConstant.sort_Category_By, required = false) String sortBy,
            @RequestParam(name = "sortOrder", defaultValue = AppConstant.Sort_DIR, required = false) String sortOrder
    ) {
       CategoryResponse categoryResponse =categoryService.getAllCategories(pageNumber,pageSize,sortBy,sortOrder);
       return new ResponseEntity<>(categoryResponse,HttpStatus.OK);
    }

    @PostMapping("/public/categories")
    public ResponseEntity<CategoryDTO> createCategory(@Valid @RequestBody CategoryDTO categoryDTO) {
        CategoryDTO categoryDTOToReturn=categoryService.createCategory(categoryDTO);
        return new ResponseEntity<>(categoryDTOToReturn,HttpStatus.CREATED);
    }

    @DeleteMapping("/admin/categories/{categoryId}")
    public ResponseEntity<CategoryDTO> deleteCategory(@PathVariable Long categoryId){
            CategoryDTO deleteCategory = categoryService.deleteCategory(categoryId);
            return new ResponseEntity<>(deleteCategory, HttpStatus.OK);
    }

    @PutMapping("/admin/categories/{categoryId}")
    public ResponseEntity<CategoryDTO> updateCategory(@Valid @RequestBody CategoryDTO categoryDTO, @PathVariable Long categoryId){
            CategoryDTO updatedCategory = categoryService.updateCategory(categoryDTO,categoryId);
            return new ResponseEntity<>(updatedCategory,HttpStatus.OK);
    }

}
