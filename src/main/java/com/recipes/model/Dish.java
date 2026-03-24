package com.recipes.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Entity
@Table(name = "dishes")
@Getter
@Setter
public class Dish {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank
    @Size(min = 2)
    @Column(nullable = false)
    private String name;

    @ElementCollection
    @CollectionTable(name = "dish_photos", joinColumns = @JoinColumn(name = "dish_id"))
    @Column(name = "photo_url")
    private List<String> photos = new ArrayList<>();

    @NotNull
    @Min(0)
    @Column(nullable = false)
    private Double calories;

    @NotNull
    @Min(0)
    @Column(nullable = false)
    private Double proteins;

    @NotNull
    @Min(0)
    @Column(nullable = false)
    private Double fats;

    @NotNull
    @Min(0)
    @Column(nullable = false)
    private Double carbohydrates;

    @OneToMany(mappedBy = "dish", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<DishIngredient> ingredients = new ArrayList<>();

    @NotNull
    @Min(value = 1)
    @Column(nullable = false)
    private Double portionSize;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private DishCategory category;

    @ElementCollection
    @CollectionTable(name = "dish_flags", joinColumns = @JoinColumn(name = "dish_id"))
    @Enumerated(EnumType.STRING)
    @Column(name = "flag")
    private Set<DietFlag> flags = new HashSet<>();

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    private LocalDateTime updatedAt;

    public enum DishCategory {
        NONE, DESSERT, FIRST_COURSE, SECOND_COURSE, DRINK, SALAD, SOUP, SNACK
    }
}
