package com.recipes.service;

import com.recipes.dto.DishDTO;
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
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class DishService {

    private final DishRepository dishRepository;
    private final ProductRepository productRepository;

    private static final Map<String, Dish.DishCategory> MACROS = new LinkedHashMap<>();
    static {
        MACROS.put("!десерт", Dish.DishCategory.DESSERT);
        MACROS.put("!первое", Dish.DishCategory.FIRST_COURSE);
        MACROS.put("!второе", Dish.DishCategory.SECOND_COURSE);
        MACROS.put("!напиток", Dish.DishCategory.DRINK);
        MACROS.put("!салат", Dish.DishCategory.SALAD);
        MACROS.put("!суп", Dish.DishCategory.SOUP);
        MACROS.put("!перекус", Dish.DishCategory.SNACK);
    }

    @Transactional(readOnly = true)
    public List<Dish> findAll(String name, Dish.DishCategory category, List<DietFlag> flags) {
        List<DietFlag> safeFlags = flags != null ? flags : List.of();
        long flagsCount = safeFlags.size();
        if (safeFlags.isEmpty()) safeFlags = List.of(DietFlag.VEGAN);
        return dishRepository.findWithFilters(name, category, safeFlags, flagsCount);
    }

    @Transactional(readOnly = true)
    public Dish findById(Long id) {
        return dishRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Dish not found: " + id));
    }

    @Transactional
    public Dish create(DishDTO.Request dto) {
        validateNutrition(dto);
        Dish dish = new Dish();
        applyRequest(dish, dto);
        return dishRepository.save(dish);
    }

    @Transactional
    public Dish update(Long id, DishDTO.Request dto) {
        validateNutrition(dto);
        Dish dish = findById(id);
        dish.getIngredients().clear();
        applyRequest(dish, dto);
        return dishRepository.save(dish);
    }

    @Transactional
    public void delete(Long id) {
        Dish dish = findById(id);
        dishRepository.delete(dish);
    }

    public DishDTO.NutritionCalculation calculateNutrition(List<DishDTO.IngredientRequest> ingredientRequests) {
        List<Product> products = resolveProducts(ingredientRequests);
        double calories = calculateNutrientSum(products, ingredientRequests, "calories");
        double proteins = calculateNutrientSum(products, ingredientRequests, "proteins");
        double fats = calculateNutrientSum(products, ingredientRequests, "fats");
        double carbs = calculateNutrientSum(products, ingredientRequests, "carbohydrates");

        if (calories < 0 || proteins < 0 || fats < 0 || carbs < 0) {
            throw new IllegalStateException("Расчет КБЖУ невозможен: итоговые значения не могут быть отрицательными.");
        }

        DishDTO.NutritionCalculation calc = new DishDTO.NutritionCalculation();
        calc.setCalories(calories);
        calc.setProteins(proteins);
        calc.setFats(fats);
        calc.setCarbohydrates(carbs);
        calc.setAvailableFlags(calculateAvailableFlags(products));
        return calc;
    }

    private void applyRequest(Dish dish, DishDTO.Request dto) {
        String name = dto.getName();
        Dish.DishCategory categoryFromMacro = null;

        for (Map.Entry<String, Dish.DishCategory> entry : MACROS.entrySet()) {
            if (name.contains(entry.getKey())) {
                categoryFromMacro = entry.getValue();
                name = name.replace(entry.getKey(), "").trim();
                break;
            }
        }

        dish.setName(name);
        dish.setPhotos(dto.getPhotos());
        dish.setPortionSize(dto.getPortionSize());

        Dish.DishCategory resolvedCategory;
        if (dto.getCategory() != null && dto.getCategory() != Dish.DishCategory.NONE) {
            resolvedCategory = dto.getCategory();
        } else if (categoryFromMacro != null) {
            resolvedCategory = categoryFromMacro;
        } else {
            throw new IllegalStateException("Категория --- выбрана, но макрос в названии не найден.");
        }
        dish.setCategory(resolvedCategory);

        List<Product> products = resolveProducts(dto.getIngredients());
        for (int i = 0; i < products.size(); i++) {
            DishIngredient ingredient = new DishIngredient();
            ingredient.setDish(dish);
            ingredient.setProduct(products.get(i));
            ingredient.setQuantity(dto.getIngredients().get(i).getQuantity());
            dish.getIngredients().add(ingredient);
        }

        double autoCalories = calculateNutrientSum(products, dto.getIngredients(), "calories");
        double autoProteins = calculateNutrientSum(products, dto.getIngredients(), "proteins");
        double autoFats = calculateNutrientSum(products, dto.getIngredients(), "fats");
        double autoCarbs = calculateNutrientSum(products, dto.getIngredients(), "carbohydrates");

        dish.setCalories(dto.getCalories() != null ? dto.getCalories() : autoCalories);
        dish.setProteins(dto.getProteins() != null ? dto.getProteins() : autoProteins);
        dish.setFats(dto.getFats() != null ? dto.getFats() : autoFats);
        dish.setCarbohydrates(dto.getCarbohydrates() != null ? dto.getCarbohydrates() : autoCarbs);

        Set<DietFlag> availableFlags = calculateAvailableFlags(products);
        Set<DietFlag> requestedFlags = dto.getFlags() != null ? dto.getFlags() : new HashSet<>();
        Set<DietFlag> validFlags = requestedFlags.stream()
                .filter(availableFlags::contains)
                .collect(Collectors.toSet());
        dish.setFlags(validFlags);
    }

    private void validateNutrition(DishDTO.Request dto) {
        if (dto == null) return;
        Double p = dto.getProteins();
        Double f = dto.getFats();
        Double c = dto.getCarbohydrates();
        if (p == null || f == null || c == null) return;
        if (p < 0 || f < 0 || c < 0) {
            throw new IllegalStateException("Белки, жиры и углеводы блюда должны быть >= 0 г.");
        }
        if (p > 100 || f > 100 || c > 100) {
            throw new IllegalStateException("Каждый компонент БЖУ блюда должен быть <= 100 г.");
        }
        double sum = p + f + c;
        if (sum > 100) {
            throw new IllegalStateException("Сумма белков, жиров и углеводов блюда должна быть <= 100 г. Текущая сумма: "
                    + String.format("%.1f", sum) + " г.");
        }
    }

    private double calculateNutrientSum(List<Product> products,
                                        List<DishDTO.IngredientRequest> requests,
                                        String nutrient) {
        double sum = 0;
        for (int i = 0; i < products.size(); i++) {
            Product p = products.get(i);
            double quantity = requests.get(i).getQuantity();
            double per100 = switch (nutrient) {
                case "calories" -> p.getCalories();
                case "proteins" -> p.getProteins();
                case "fats" -> p.getFats();
                case "carbohydrates" -> p.getCarbohydrates();
                default -> 0;
            };
            sum += per100 * quantity / 100.0;
        }
        return Math.round(sum * 100.0) / 100.0;
    }

    private Set<DietFlag> calculateAvailableFlags(List<Product> products) {
        if (products.isEmpty()) return new HashSet<>();
        Set<DietFlag> available = new HashSet<>(EnumSet.allOf(DietFlag.class));
        for (Product p : products) {
            available.retainAll(p.getFlags());
        }
        return available;
    }

    private List<Product> resolveProducts(List<DishDTO.IngredientRequest> requests) {
        return requests.stream()
                .map(r -> productRepository.findById(r.getProductId())
                        .orElseThrow(() -> new EntityNotFoundException("Product not found: " + r.getProductId())))
                .collect(Collectors.toList());
    }

    public DishDTO.Response toResponse(Dish dish) {
        DishDTO.Response r = new DishDTO.Response();
        r.setId(dish.getId());
        r.setName(dish.getName());
        r.setPhotos(dish.getPhotos());
        r.setCalories(dish.getCalories());
        r.setProteins(dish.getProteins());
        r.setFats(dish.getFats());
        r.setCarbohydrates(dish.getCarbohydrates());
        r.setPortionSize(dish.getPortionSize());
        r.setCategory(dish.getCategory());
        r.setFlags(dish.getFlags());
        r.setCreatedAt(dish.getCreatedAt());
        r.setUpdatedAt(dish.getUpdatedAt());

        List<DishDTO.IngredientResponse> ingredientResponses = dish.getIngredients().stream().map(di -> {
            DishDTO.IngredientResponse ir = new DishDTO.IngredientResponse();
            ir.setProductId(di.getProduct().getId());
            ir.setProductName(di.getProduct().getName());
            ir.setQuantity(di.getQuantity());
            return ir;
        }).collect(Collectors.toList());
        r.setIngredients(ingredientResponses);

        return r;
    }
}
