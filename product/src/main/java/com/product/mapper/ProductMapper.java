package com.product.mapper;

import com.product.dto.ProductDto;
import com.product.dto.BrandDto;
import com.product.dto.CategoryDto;
import com.product.dto.ImageDto;
import com.product.dto.SizeDto;
import com.product.dto.SubCategoryDto;
import com.product.entity.Brand;
import com.product.entity.Image;
import com.product.entity.Product;
import com.product.entity.Size;

import java.util.LinkedHashSet;
import java.util.function.Function;

public final class ProductMapper {

    private ProductMapper() {
    }

    public static ProductDto convertProductToProductDto(
            Product product, Function<String, String> imageUrlFactory) {
        ProductDto productDto = new ProductDto();
        productDto.setId(product.getId());
        productDto.setName(product.getName());

        if (product.getSubCategory() != null) {
            var subCategory = product.getSubCategory();
            SubCategoryDto subCategoryDto = new SubCategoryDto();
            subCategoryDto.setId(subCategory.getId());
            subCategoryDto.setName(subCategory.getName());

            if (subCategory.getCategory() != null) {
                var category = subCategory.getCategory();
                subCategoryDto.setCategory(new CategoryDto(category.getId(), category.getName()));
            }

            productDto.setSubCategory(subCategoryDto);
        }

        for (Brand brand : product.getBrands()) {
            productDto.getBrands().add(convertBrandToBrandDto(brand, imageUrlFactory));
        }

        return productDto;
    }

    public static BrandDto convertBrandToBrandDto(
            Brand brand, Function<String, String> imageUrlFactory) {
        BrandDto brandDto = new BrandDto();
        brandDto.setId(brand.getId());
        brandDto.setName(brand.getName());
        brandDto.setPrice(brand.getPrice());

        for (Size size : brand.getSizes()) {
            brandDto.getSizes().add(new SizeDto(size.getId(), size.getSize(), size.getQuantity()));
        }
        for (Image image : brand.getImages()) {
            // Convert the database key to a temporary URL for the API response.
            brandDto.getImages().add(new ImageDto(image.getId(), imageUrlFactory.apply(image.getUrl())));
        }

        return brandDto;
    }
}
