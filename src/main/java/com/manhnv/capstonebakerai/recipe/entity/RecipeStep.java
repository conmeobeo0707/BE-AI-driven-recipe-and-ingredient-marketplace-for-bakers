package com.manhnv.capstonebakerai.recipe.entity;

import com.manhnv.capstonebakerai.recipe.enums.RecipeStepType;
import jakarta.persistence.*;
import org.hibernate.annotations.ColumnDefault;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;
import lombok.EqualsAndHashCode;

import java.util.UUID;

@Entity
@Table(name = "recipe_steps")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RecipeStep {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "recipe_id", nullable = false)
    private Recipe recipe;

    @NotNull
    @Column(name = "step_number", nullable = false)
    private Integer stepNumber;

    @Column(name = "instruction")
    private String instruction;

    @Builder.Default
    @ColumnDefault("0")
    @Column(name = "duration_minutes", nullable = false)
    private Integer durationMinutes = 0;

    @Column(name = "notes", columnDefinition = "TEXT")
    private String notes;

    @Builder.Default
    @Enumerated(EnumType.STRING)
    @ColumnDefault("'COOKING'")
    @Column(name = "step_type", nullable = false)
    private RecipeStepType recipeStepType = RecipeStepType.COOKING;

    @OneToMany(mappedBy = "recipeStep", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private java.util.List<RecipeStepImage> images;

    @OneToMany(mappedBy = "recipeStep", fetch = FetchType.LAZY)
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private java.util.List<RecipeStepIngredient> ingredients;

    @OneToMany(mappedBy = "recipeStep", fetch = FetchType.LAZY)
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private java.util.List<RecipeStepTool> tools;
}
