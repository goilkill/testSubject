package com.recipes.dto;

import com.recipes.model.DietFlag;
import com.recipes.model.Dish;
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

public class DishDTO {

    @Data
    public static class IngredientRequest {
        @NotNull
        private Long productId;

        @NotNull
        @Min(0)
        private Double quantity;
    }

    @Data
    public static class IngredientResponse {
        private Long productId;
        private String productName;
        private Double quantity;
    }

    @Data
    public static class Request {
        @NotBlank
        @Size(min = 2)
        private String name;

        private List<String> photos = new ArrayList<>();

        private Double calories;
        private Double proteins;
        private Double fats;
        private Double carbohydrates;

        @NotNull
        @Size(min = 1)
        private List<IngredientRequest> ingredients;

        @NotNull
        @Min(1)
        private Double portionSize;

        @NotNull
        private Dish.DishCategory category;

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
        private List<IngredientResponse> ingredients;
        private Double portionSize;
        private Dish.DishCategory category;
        private Set<DietFlag> flags;
        private LocalDateTime createdAt;
        private LocalDateTime updatedAt;
    }

    @Data
    public static class NutritionCalculation {
        private Double calories;
        private Double proteins;
        private Double fats;
        private Double carbohydrates;
        private Set<DietFlag> availableFlags;
    }
}
