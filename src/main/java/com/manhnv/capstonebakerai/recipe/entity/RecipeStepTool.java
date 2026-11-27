package com.manhnv.capstonebakerai.recipe.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

import java.math.BigDecimal;
import java.util.UUID;

@Entity
@Table(name = "recipe_step_tools", uniqueConstraints = @UniqueConstraint(
        name = "uk_recipe_step_tools_step_recipe_tool",
        columnNames = {"recipe_step_id", "recipe_tool_id"}))
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RecipeStepTool {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "recipe_step_id", nullable = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    private RecipeStep recipeStep;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "recipe_tool_id", nullable = false,
            foreignKey = @ForeignKey(name = "fk_recipe_step_tools_recipe_tool"))
    @OnDelete(action = OnDeleteAction.CASCADE)
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private RecipeTool recipeTool;

    @Column(name = "quantity", precision = 10, scale = 2)
    private BigDecimal quantity;

    @Column(name = "note", columnDefinition = "TEXT")
    private String note;
}
