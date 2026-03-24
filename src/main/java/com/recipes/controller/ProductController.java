package com.recipes.controller;

import com.recipes.dto.ProductDTO;
import com.recipes.model.DietFlag;
import com.recipes.model.Product;
import com.recipes.service.ProductService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Comparator;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/products")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class ProductController {

    private final ProductService productService;

    @GetMapping
    public List<ProductDTO.Response> getAll(
            @RequestParam(required = false) String name,
            @RequestParam(required = false) Product.ProductCategory category,
            @RequestParam(required = false) Product.CookingStatus cookingStatus,
            @RequestParam(required = false) List<DietFlag> flags,
            @RequestParam(required = false, defaultValue = "name") String sortBy
    ) {
        List<Product> products = productService.findAll(name, category, cookingStatus, flags);

        Comparator<Product> comparator = switch (sortBy) {
            case "calories" -> Comparator.comparingDouble(Product::getCalories);
            case "proteins" -> Comparator.comparingDouble(Product::getProteins);
            case "fats"     -> Comparator.comparingDouble(Product::getFats);
            case "carbs"    -> Comparator.comparingDouble(Product::getCarbohydrates);
            default         -> Comparator.comparing(Product::getName, String.CASE_INSENSITIVE_ORDER);
        };
        products.sort(comparator);

        return products.stream().map(productService::toResponse).toList();
    }

    @GetMapping("/{id}")
    public ProductDTO.Response getById(@PathVariable Long id) {
        return productService.toResponse(productService.findById(id));
    }

    @PostMapping
    public ProductDTO.Response create(@Valid @RequestBody ProductDTO.Request dto) {
        return productService.toResponse(productService.create(dto));
    }

    @PutMapping("/{id}")
    public ProductDTO.Response update(@PathVariable Long id, @Valid @RequestBody ProductDTO.Request dto) {
        return productService.toResponse(productService.update(id, dto));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> delete(@PathVariable Long id) {
        try {
            productService.delete(id);
            return ResponseEntity.noContent().build();
        } catch (IllegalStateException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/{id}/check-deletion")
    public Map<String, Object> checkDeletion(@PathVariable Long id) {
        return productService.checkDeletion(id);
    }
}
