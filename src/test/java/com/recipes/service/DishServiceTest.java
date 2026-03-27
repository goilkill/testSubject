package com.recipes.service;

import com.recipes.dto.DishDTO;
import com.recipes.model.DietFlag;
import com.recipes.model.Product;
import com.recipes.repository.DishRepository;
import com.recipes.repository.ProductRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import jakarta.persistence.EntityNotFoundException;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DishServiceTest {

    @Mock
    private ProductRepository productRepositoryMock;

    @InjectMocks
    private DishService dishServiceMock;

    @BeforeEach
    void resetStubs() {

    }

    private static Product productWithNutrition(double calories, double proteins, double fats, double carbs, DietFlag... flags) {
        Product p = new Product();
        p.setCalories(calories);
        p.setProteins(proteins);
        p.setFats(fats);
        p.setCarbohydrates(carbs);
        p.setFlags(flags.length == 0 ? EnumSet.noneOf(DietFlag.class) : EnumSet.copyOf(List.of(flags)));
        return p;
    }

    private static DishDTO.IngredientRequest ingredient(long productId, double quantityGrams) {
        DishDTO.IngredientRequest r = new DishDTO.IngredientRequest();
        r.setProductId(productId);
        r.setQuantity(quantityGrams);
        return r;
    }

    @Test
    @DisplayName("Equivalence: пустой список ингредиентов -> калории = 0.0")
    void calculateNutrition_emptyIngredients() {
        List<DishDTO.IngredientRequest> ingredients = List.of();

        var calc = dishServiceMock.calculateNutrition(ingredients);

        assertNotNull(calc.getCalories());
        assertEquals(0.0, calc.getCalories(), 0.0001);
        assertNotNull(calc.getAvailableFlags());
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("caloriesCases")
    @DisplayName("Equivalence partitions + boundary values: проверка авто-расчёта калорий")
    void calculateNutrition_calories_equivalenceAndBoundaries(
            String caseName,
            List<DishDTO.IngredientRequest> ingredientRequests,
            Map<Long, Product> productsById,
            double expectedCalories
    ) {
        for (var e : productsById.entrySet()) {
            when(productRepositoryMock.findById(e.getKey())).thenReturn(Optional.of(e.getValue()));
        }

        var calc = dishServiceMock.calculateNutrition(ingredientRequests);

        assertNotNull(calc.getCalories(), "DishDTO.NutritionCalculation.calories должен быть заполнен");
        assertEquals(expectedCalories, calc.getCalories(), 0.0001, "Калории должны совпасть с формулой Σ(per100 * quantity / 100)");
    }

    private static Stream<Arguments> caloriesCases() {


        var i0 = List.of(ingredient(1L, 0.0));
        var p0 = Map.of(1L, productWithNutrition(123.45, 0, 0, 0));
        Arguments a0 = Arguments.of("q=0 => 0", i0, p0, 0.0);


        var i1 = List.of(ingredient(1L, 50.0));
        var p1 = Map.of(1L, productWithNutrition(200.0, 0, 0, 0));
        Arguments a1 = Arguments.of("q=50 => per100/2", i1, p1, 100.0);


        var i2 = List.of(ingredient(1L, 100.0));
        var p2 = Map.of(1L, productWithNutrition(87.25, 0, 0, 0));
        Arguments a2 = Arguments.of("q=100 => per100", i2, p2, 87.25);

        var i3 = List.of(ingredient(1L, 3.0));
        var p3 = Map.of(1L, productWithNutrition(33.333, 0, 0, 0));
        Arguments a3 = Arguments.of("rounding to 2 decimals", i3, p3, 1.0);

        var i4 = List.of(
            ingredient(1L, 50.0),
            ingredient(2L, 25.0)
        );
        var p4 = Map.of(
            1L, productWithNutrition(100.0, 0, 0, 0),
            2L, productWithNutrition(200.0, 0, 0, 0)
        );
        Arguments a4 = Arguments.of("sum of multiple ingredients", i4, p4, 100.0);

        var i5 = List.of(ingredient(1L, 1.0));
        var p5 = Map.of(1L, productWithNutrition(Double.MAX_VALUE, 0, 0, 0));
        double sum5 = Double.MAX_VALUE * 1.0 / 100.0;
        double expected5 = Math.round(sum5 * 100.0) / 100.0;
        Arguments a5 = Arguments.of("Double.MAX_VALUE boundary (no crash)", i5, p5, expected5);

        return Stream.of(a0, a1, a2, a3, a4, a5);
    }

    @Test
    @DisplayName("Граница/ошибка: если продукт не найден -> EntityNotFoundException")
    void calculateNutrition_productNotFound_throws() {
        List<DishDTO.IngredientRequest> ingredients = List.of(ingredient(999L, 10.0));
        when(productRepositoryMock.findById(999L)).thenReturn(Optional.empty());

        assertThrows(EntityNotFoundException.class, () -> dishServiceMock.calculateNutrition(ingredients));
    }

    @Test
    @DisplayName("Negative case: отрицательное количество ингредиента -> IllegalStateException")
    void calculateNutrition_negativeQuantity_throws() {
        List<DishDTO.IngredientRequest> ingredients = List.of(ingredient(1L, -50.0));
        when(productRepositoryMock.findById(1L))
                .thenReturn(Optional.of(productWithNutrition(200.0, 0, 0, 0)));

        assertThrows(IllegalStateException.class, () -> dishServiceMock.calculateNutrition(ingredients));
    }
}