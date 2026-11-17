package com.product.mapper;

import com.product.dto.CategoryDto;
import com.product.entity.Category;
import org.modelmapper.ModelMapper;

public class CategoryMapper {
    public static final ModelMapper mapper = new ModelMapper();

    public static CategoryDto convertCategoryToCategoryDto(Category category){
        return mapper.map(category,CategoryDto.class);
    }
}
