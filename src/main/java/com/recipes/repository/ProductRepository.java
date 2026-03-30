package com.recipes.repository;

import com.recipes.model.DietFlag;
import com.recipes.model.Product;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ProductRepository extends JpaRepository<Product, Long> {

    List<Product> findByNameContainingIgnoreCase(String name);

    // Для PostgreSQL: ILIKE надёжнее для кириллицы, чем LOWER(... ) LIKE LOWER(...).
    @Query("SELECT p FROM Product p WHERE p.name ILIKE CONCAT('%', :name, '%')")
    List<Product> findByNameContainingIlike(@Param("name") String name);

    List<Product> findByCategory(Product.ProductCategory category);

    List<Product> findByCookingStatus(Product.CookingStatus cookingStatus);

    @Query("SELECT DISTINCT p FROM Product p JOIN p.flags f WHERE f = :flag")
    List<Product> findByFlag(@Param("flag") DietFlag flag);

    @Query("SELECT DISTINCT p FROM Product p " +
            "WHERE (:name IS NULL OR LOWER(p.name) LIKE CONCAT(CONCAT('%', LOWER(:name)), '%')) " +
            "AND (:category IS NULL OR p.category = :category) " +
            "AND (:cookingStatus IS NULL OR p.cookingStatus = :cookingStatus) " +
            "AND (:flagsCount = 0 OR (SELECT COUNT(DISTINCT f2) FROM p.flags f2 WHERE f2 IN :flags) = :flagsCount)")
    List<Product> findWithFilters(
            @Param("name") String name,
            @Param("category") Product.ProductCategory category,
            @Param("cookingStatus") Product.CookingStatus cookingStatus,
            @Param("flags") List<DietFlag> flags,
            @Param("flagsCount") long flagsCount
    );

    @Query("SELECT COUNT(di) > 0 FROM DishIngredient di WHERE di.product.id = :productId")
    boolean isUsedInDish(@Param("productId") Long productId);

    @Query("SELECT DISTINCT di.dish.name FROM DishIngredient di WHERE di.product.id = :productId")
    List<String> findDishNamesByProductId(@Param("productId") Long productId);
}
