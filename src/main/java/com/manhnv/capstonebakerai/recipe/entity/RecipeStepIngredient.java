package com.manhnv.capstonebakerai.recipe.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

import java.math.BigDecimal;
import java.util.UUID;

@Entity
@Table(name = "recipe_step_ingredients", uniqueConstraints = @UniqueConstraint(
        name = "uk_recipe_step_ingredients_step_recipe_ingredient",
        columnNames = {"recipe_step_id", "recipe_ingredient_id"}))
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RecipeStepIngredient {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "recipe_step_id", nullable = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    private RecipeStep recipeStep;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "recipe_ingredient_id", nullable = false,
            foreignKey = @ForeignKey(name = "fk_recipe_step_ingredients_recipe_ingredient"))
    @OnDelete(action = OnDeleteAction.CASCADE)
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private RecipeIngredient recipeIngredient;

    @Column(name = "quantity", precision = 10, scale = 2)
    private BigDecimal quantity;
}
