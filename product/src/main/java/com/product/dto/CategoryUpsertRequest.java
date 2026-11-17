package com.product.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CategoryUpsertRequest(
        @NotBlank @Size(max = 45) String name) {
}
