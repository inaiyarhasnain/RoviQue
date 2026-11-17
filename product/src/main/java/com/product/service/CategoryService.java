package com.product.service;

import com.product.dto.CategoryDto;
import org.springframework.stereotype.Service;

import java.util.List;

// Marks the category service layer; CategoryServiceImpl provides the Spring-managed implementation.
@Service
public interface CategoryService {

    public List<CategoryDto> findAll();
    public CategoryDto findByCategoryId(long id);
    public CategoryDto findByCategoryName(String name);

}
