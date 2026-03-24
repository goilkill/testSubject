package com.recipes.dto;

import com.recipes.model.DietFlag;
import com.recipes.model.Product;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class ProductDTO {

    @Data
    public static class Request {
        @NotBlank
        @Size(min = 2)
        private String name;

        private List<String> photos = new ArrayList<>();

        @NotNull
        @Min(0)
        private Double calories;

        @NotNull
        @Min(0)
        private Double proteins;

        @NotNull
        @Min(0)
        private Double fats;

        @NotNull
        @Min(0)
        private Double carbohydrates;

        private String composition;

        @NotNull
        private Product.ProductCategory category;

        @NotNull
        private Product.CookingStatus cookingStatus;

        private Set<DietFlag> flags = new HashSet<>();
    }

    @Data
    public static class Response {
        private Long id;
        private String name;
        private List<String> photos;
        private Double calories;
        private Double proteins;
        private Double fats;
        private Double carbohydrates;
        private String composition;
        private Product.ProductCategory category;
        private Product.CookingStatus cookingStatus;
        private Set<DietFlag> flags;
        private LocalDateTime createdAt;
        private LocalDateTime updatedAt;
    }
}
