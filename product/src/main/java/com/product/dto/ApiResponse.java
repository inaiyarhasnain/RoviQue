package com.product.dto;

import lombok.Getter;
import lombok.Setter;

// Lombok creates methods for reading and updating the response fields.
@Getter
@Setter
public class ApiResponse<T> {
    private String message;
    private int status;
    private T data;

}
