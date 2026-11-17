package com.product.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

public record ProductUpsertRequest(
        @NotBlank @Size(max = 45) String name,
        @NotNull @Positive Integer subCategoryId) {
}
