package com.recipes.controller;

import com.recipes.dto.DishDTO;
import com.recipes.model.DietFlag;
import com.recipes.model.Dish;
import com.recipes.service.DishService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/dishes")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class DishController {

    private final DishService dishService;

    @GetMapping
    public List<DishDTO.Response> getAll(
            @RequestParam(required = false) String name,
            @RequestParam(required = false) Dish.DishCategory category,
            @RequestParam(required = false) List<DietFlag> flags
    ) {
        return dishService.findAll(name, category, flags)
                .stream().map(dishService::toResponse).toList();
    }

    @GetMapping("/{id}")
    public DishDTO.Response getById(@PathVariable Long id) {
        return dishService.toResponse(dishService.findById(id));
    }

    @PostMapping
    public DishDTO.Response create(@Valid @RequestBody DishDTO.Request dto) {
        return dishService.toResponse(dishService.create(dto));
    }

    @PutMapping("/{id}")
    public DishDTO.Response update(@PathVariable Long id, @Valid @RequestBody DishDTO.Request dto) {
        return dishService.toResponse(dishService.update(id, dto));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        dishService.delete(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/calculate")
    public DishDTO.NutritionCalculation calculate(@RequestBody List<DishDTO.IngredientRequest> ingredients) {
        return dishService.calculateNutrition(ingredients);
    }
}
