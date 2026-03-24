package com.recipes.service;

import com.recipes.dto.ProductDTO;
import com.recipes.model.DietFlag;
import com.recipes.model.Dish;
import com.recipes.model.DishIngredient;
import com.recipes.model.Product;
import com.recipes.repository.DishRepository;
import com.recipes.repository.ProductRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

@Service
@RequiredArgsConstructor
public class ProductService {

    private final ProductRepository productRepository;
    private final DishRepository dishRepository;

    @Transactional(readOnly = true)
    public List<Product> findAll(String name,
                                   Product.ProductCategory category,
                                   Product.CookingStatus cookingStatus,
                                   List<DietFlag> flags) {
        List<DietFlag> safeFlags = flags != null ? flags : List.of();
        long flagsCount = safeFlags.size();
        if (safeFlags.isEmpty()) safeFlags = List.of(DietFlag.VEGAN);
        return productRepository.findWithFilters(name, category, cookingStatus, safeFlags, flagsCount);
    }

    @Transactional(readOnly = true)
    public Product findById(Long id) {
        return productRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Product not found: " + id));
    }

    @Transactional
    public Product create(ProductDTO.Request dto) {
        validateNutrition(dto);
        Product product = new Product();
        mapDtoToProduct(dto, product);
        return productRepository.save(product);
    }

    @Transactional
    public Product update(Long id, ProductDTO.Request dto) {
        validateNutrition(dto);
        Product product = findById(id);
        mapDtoToProduct(dto, product);
        Product saved = productRepository.save(product);

        recalculateDishFlags(id);

        return saved;
    }

    private void recalculateDishFlags(Long productId) {
        List<Dish> dishes = dishRepository.findDishesByProductId(productId);
        for (Dish dish : dishes) {
            List<Product> products = dish.getIngredients()
                    .stream()
                    .map(DishIngredient::getProduct)
                    .toList();

            Set<DietFlag> available = new HashSet<>(EnumSet.allOf(DietFlag.class));
            for (Product p : products) {
                available.retainAll(p.getFlags());
            }

            dish.getFlags().retainAll(available);
            dishRepository.save(dish);
        }
    }

    @Transactional
    public void delete(Long id) {
        Product product = findById(id);

        if (productRepository.isUsedInDish(id)) {
            List<String> dishNames = productRepository.findDishNamesByProductId(id);
            throw new IllegalStateException(
                "Cannot delete product '" + product.getName() + "' because it is used in dishes: " + dishNames
            );
        }

        productRepository.delete(product);
    }

    public Map<String, Object> checkDeletion(Long id) {
        boolean usedInDish = productRepository.isUsedInDish(id);
        List<String> dishNames = usedInDish ? productRepository.findDishNamesByProductId(id) : List.of();
        return Map.of("canDelete", !usedInDish, "usedInDishes", dishNames);
    }

    private void validateNutrition(ProductDTO.Request dto) {
        if (dto == null) return;
        Double p = dto.getProteins();
        Double f = dto.getFats();
        Double c = dto.getCarbohydrates();
        if (p == null || f == null || c == null) return;
        if (p < 0 || f < 0 || c < 0) {
            throw new IllegalStateException("Белки, жиры и углеводы должны быть ≥ 0 г на 100 г.");
        }
        if (p > 100 || f > 100 || c > 100) {
            throw new IllegalStateException("Каждый компонент БЖУ должен быть ≤ 100 г на 100 г.");
        }
        double sum = p + f + c;
        if (sum > 100) {
            throw new IllegalStateException("Сумма белков, жиров и углеводов должна быть ≤ 100 г на 100 г. Текущая сумма: " + String.format("%.1f", sum) + " г.");
        }
    }

    private void mapDtoToProduct(ProductDTO.Request dto, Product product) {
        product.setName(dto.getName());
        product.setPhotos(dto.getPhotos());
        product.setCalories(dto.getCalories());
        product.setProteins(dto.getProteins());
        product.setFats(dto.getFats());
        product.setCarbohydrates(dto.getCarbohydrates());
        product.setComposition(dto.getComposition());
        product.setCategory(dto.getCategory());
        product.setCookingStatus(dto.getCookingStatus());
        product.setFlags(dto.getFlags());
    }

    public ProductDTO.Response toResponse(Product product) {
        ProductDTO.Response r = new ProductDTO.Response();
        r.setId(product.getId());
        r.setName(product.getName());
        r.setPhotos(product.getPhotos());
        r.setCalories(product.getCalories());
        r.setProteins(product.getProteins());
        r.setFats(product.getFats());
        r.setCarbohydrates(product.getCarbohydrates());
        r.setComposition(product.getComposition());
        r.setCategory(product.getCategory());
        r.setCookingStatus(product.getCookingStatus());
        r.setFlags(product.getFlags());
        r.setCreatedAt(product.getCreatedAt());
        r.setUpdatedAt(product.getUpdatedAt());
        return r;
    }
}
