package com.recipes.repository;

import com.recipes.model.DietFlag;
import com.recipes.model.Dish;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface DishRepository extends JpaRepository<Dish, Long> {

    List<Dish> findByNameContainingIgnoreCase(String name);

    List<Dish> findByCategory(Dish.DishCategory category);

    @Query("SELECT DISTINCT d FROM Dish d JOIN d.flags f WHERE f = :flag")
    List<Dish> findByFlag(@Param("flag") DietFlag flag);

    @Query("SELECT DISTINCT d FROM Dish d " +
            "WHERE (:name IS NULL OR LOWER(d.name) LIKE LOWER(CONCAT('%', CAST(:name AS string), '%'))) " +
            "AND (:category IS NULL OR d.category = :category) " +
            "AND (:flagsCount = 0 OR (SELECT COUNT(DISTINCT f2) FROM d.flags f2 WHERE f2 IN :flags) = :flagsCount)")
    List<Dish> findWithFilters(
            @Param("name") String name,
            @Param("category") Dish.DishCategory category,
            @Param("flags") List<DietFlag> flags,
            @Param("flagsCount") long flagsCount
    );

    @Query("SELECT DISTINCT di.dish FROM DishIngredient di WHERE di.product.id = :productId")
    List<Dish> findDishesByProductId(@Param("productId") Long productId);
}
