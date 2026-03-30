package com.recipes.api;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.recipes.dto.DishDTO;
import com.recipes.model.Dish;
import com.recipes.model.Product;
import com.recipes.repository.DishRepository;
import com.recipes.repository.ProductRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.closeTo;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;


@SpringBootTest
@ActiveProfiles("test")
@AutoConfigureMockMvc
@Transactional
class DishApiIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private DishRepository dishRepository;

    private Long productId;

    @BeforeEach
    void setUp() {
        Product p = new Product();
        p.setName("Интеграционный продукт");
        p.setCalories(365.0);
        p.setProteins(12.0);
        p.setFats(10.0);
        p.setCarbohydrates(58.0);
        p.setComposition("test");
        p.setCategory(Product.ProductCategory.GRAINS);
        p.setCookingStatus(Product.CookingStatus.READY_TO_EAT);
        p.setPhotos(new ArrayList<>());
        productId = productRepository.save(p).getId();
        productRepository.flush();
    }

    @Test
    @DisplayName("ЭР: пустой список ингредиентов -> 200, калории 0")
    void calculate_emptyIngredients_returnsZeroCalories() throws Exception {
        mockMvc.perform(post("/api/dishes/calculate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("[]"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.calories").value(0.0))
                .andExpect(jsonPath("$.proteins").value(0.0))
                .andExpect(jsonPath("$.fats").value(0.0))
                .andExpect(jsonPath("$.carbohydrates").value(0.0));
    }

    private static Stream<Arguments> calculateCaloriesCases() {
        return Stream.of(
                Arguments.of("граница: quantity = 0", 0.0, 0.0),
                Arguments.of("граница: quantity = 100 (весь пер 100 г)", 100.0, 365.0),
                Arguments.of("внутри класса: quantity = 50", 50.0, 182.5),
                Arguments.of("несколько порций сумации через одну позицию", 200.0, 730.0)
        );
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("calculateCaloriesCases")
    @DisplayName("ЭР + границы: POST /calculate с реальным продуктом из БД")
    void calculate_withSavedProduct_returnsExpectedCalories(String caseName, double quantity, double expectedCalories)
            throws Exception {
        List<DishDTO.IngredientRequest> body = List.of(ingredient(productId, quantity));
        mockMvc.perform(post("/api/dishes/calculate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.calories", closeTo(expectedCalories, 0.01)));
    }

    @Test
    @DisplayName("Негатив: несуществующий productId -> 404 и тело error")
    void calculate_unknownProduct_returnsNotFound() throws Exception {
        List<DishDTO.IngredientRequest> body = List.of(ingredient(999_999L, 10.0));
        mockMvc.perform(post("/api/dishes/calculate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").exists());
    }

    @Test
    @DisplayName("Негатив: отрицательное quantity -> 400 (ошибка расчёта КБЖУ)")
    void calculate_negativeQuantity_returnsBadRequest() throws Exception {
        List<DishDTO.IngredientRequest> body = List.of(ingredient(productId, -10.0));
        mockMvc.perform(post("/api/dishes/calculate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").exists());
    }

    @Test
    @DisplayName("ЭР: поиск блюд по name без учёта регистра")
    void listDishes_nameFilter_isCaseInsensitive() throws Exception {
        Dish dish = new Dish();
        dish.setName("Борщ Особый");
        dish.setCalories(80.0);
        dish.setProteins(5.0);
        dish.setFats(3.0);
        dish.setCarbohydrates(10.0);
        dish.setPortionSize(250.0);
        dish.setCategory(Dish.DishCategory.SOUP);
        dish.setPhotos(new ArrayList<>());
        dishRepository.save(dish);
        dishRepository.flush();

        mockMvc.perform(get("/api/dishes").param("name", "бОрЩ"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", not(empty())))
                .andExpect(jsonPath("$[*].name", hasItem("Борщ Особый")));
    }

    private static DishDTO.IngredientRequest ingredient(Long productId, double quantity) {
        DishDTO.IngredientRequest r = new DishDTO.IngredientRequest();
        r.setProductId(productId);
        r.setQuantity(quantity);
        return r;
    }
}
