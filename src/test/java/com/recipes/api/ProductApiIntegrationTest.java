package com.recipes.api;

import com.recipes.model.Product;
import com.recipes.repository.ProductRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;

import static org.hamcrest.Matchers.hasItem;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;


@SpringBootTest
@ActiveProfiles("test")
@AutoConfigureMockMvc
@Transactional
class ProductApiIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ProductRepository productRepository;

    @BeforeEach
    void setUp() {
        Product p = new Product();
        p.setName("Слоёный творожок");
        p.setCalories(120.0);
        p.setProteins(6.0);
        p.setFats(7.0);
        p.setCarbohydrates(2.0);
        p.setComposition("test");
        p.setCategory(Product.ProductCategory.SWEETS);
        p.setCookingStatus(Product.CookingStatus.READY_TO_EAT);
        p.setPhotos(new ArrayList<>());
        productRepository.save(p);
        productRepository.flush(); 
    }

    @Test
    @DisplayName("ЭР + граница регистра: GET /api/products?name искомая часть в другом регистре")
    void listProducts_nameSearch_ignoresCase() throws Exception {
        mockMvc.perform(get("/api/products").param("name", "ТВОРОЖОК"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[*].name").value(hasItem("Слоёный творожок")));
    }
}
