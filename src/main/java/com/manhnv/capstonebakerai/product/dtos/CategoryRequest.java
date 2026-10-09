package com.manhnv.capstonebakerai.product.dtos;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
public record CategoryRequest(@NotBlank @Size(max = 255) String categoryName) { }
