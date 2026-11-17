package com.product.service;

import com.product.dto.CategoryDto;
import com.product.entity.Category;
import com.product.mapper.CategoryMapper;
import com.product.repository.CategoryRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.ArrayList;
import java.util.List;

// Register this class as a Spring service so controllers can use it.
// Register this class as the Spring service that implements CategoryService.
@Service
public class CategoryServiceImpl implements CategoryService{

    private CategoryRepository categoryRepository;

    public CategoryServiceImpl(CategoryRepository categoryRepository) {
        this.categoryRepository = categoryRepository;
    }

    // This method implements the category-list operation declared by the interface.
    @Override
    public List<CategoryDto> findAll() {
        // Load category entities from the database.
        List<Category> categories = categoryRepository.findAll();
        // Convert each entity into a DTO that the API can return.
        List<CategoryDto> dtoList = new ArrayList<>();
        for (Category c : categories){
            CategoryDto categoryDto = CategoryMapper.convertCategoryToCategoryDto(c);
            dtoList.add(categoryDto);
        }
        return dtoList;
    }

    // This method implements the category lookup by ID declared by the interface.
    @Override
    public CategoryDto findByCategoryId(long id) {
        return categoryRepository.findById(id)
                .map(CategoryMapper::convertCategoryToCategoryDto)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Category not found"));
    }

    // This method implements the category lookup by name declared by the interface.
    @Override
    public CategoryDto findByCategoryName(String name) {
        return categoryRepository.findByNameIgnoreCase(name)
                .map(CategoryMapper::convertCategoryToCategoryDto)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Category not found"));
    }
}
