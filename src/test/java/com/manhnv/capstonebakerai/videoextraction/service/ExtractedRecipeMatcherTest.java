package com.manhnv.capstonebakerai.videoextraction.service;

import com.manhnv.capstonebakerai.recipe.entity.Ingredient;
import com.manhnv.capstonebakerai.recipe.entity.RecipeCategory;
import com.manhnv.capstonebakerai.recipe.entity.Tool;
import com.manhnv.capstonebakerai.recipe.enums.RecipeStepType;
import com.manhnv.capstonebakerai.recipe.ingredientalias.entity.IngredientAlias;
import com.manhnv.capstonebakerai.recipe.ingredientalias.repository.IngredientAliasRepository;
import com.manhnv.capstonebakerai.recipe.ingredientalias.service.IngredientNormalizer;
import com.manhnv.capstonebakerai.recipe.repository.IngredientRepository;
import com.manhnv.capstonebakerai.recipe.repository.RecipeCategoryRepository;
import com.manhnv.capstonebakerai.recipe.repository.ToolRepository;
import com.manhnv.capstonebakerai.videoextraction.dto.RecipeVideoExtractionResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ExtractedRecipeMatcherTest {

    private final RecipeCategoryRepository categoryRepository = mock(RecipeCategoryRepository.class);
    private final ToolRepository toolRepository = mock(ToolRepository.class);
    private final IngredientRepository ingredientRepository = mock(IngredientRepository.class);
    private final IngredientAliasRepository aliasRepository = mock(IngredientAliasRepository.class);
    private final ExtractedRecipeMatcher matcher = new ExtractedRecipeMatcher(categoryRepository, toolRepository,
            ingredientRepository, aliasRepository, new IngredientNormalizer());

    private final Ingredient oreo = Ingredient.builder().id(UUID.randomUUID()).name("Bánh Oreo").build();
    private final Ingredient butter = Ingredient.builder().id(UUID.randomUUID()).name("Bơ lạt").build();
    private final Tool whisk = Tool.builder().id(UUID.randomUUID()).name("Phới lồng").build();
    private final RecipeCategory cake = RecipeCategory.builder().id(UUID.randomUUID()).name("Cake").build();

    @BeforeEach
    void setUp() {
        IngredientAlias alias = new IngredientAlias();
        alias.setIngredient(butter);
        alias.setAlias("unsalted butter");
        alias.setNormalizedAlias("unsalted butter");
        when(aliasRepository.findAllByNormalizedAliasIn(anyCollection())).thenReturn(List.of(alias));
        when(ingredientRepository.findAllByOrderByNameAscIdAsc()).thenReturn(List.of(oreo, butter));
        when(toolRepository.findAll()).thenReturn(List.of(whisk));
    }

    @Test
    void matchesByNameAndAliasAndLeavesTheRestUnmatched() {
        when(categoryRepository.findAll()).thenReturn(List.of(cake));

        RecipeVideoExtractionResponse result = matcher.match(UUID.randomUUID(), recipe("cake"), true);

        assertThat(result.category()).isEqualTo(new RecipeVideoExtractionResponse.Category("cake", cake.getId(), true, "Cake"));
        assertThat(result.ingredients()).extracting(RecipeVideoExtractionResponse.Ingredient::matched)
                .containsExactly(true, true, false, false);
        assertThat(result.ingredients().get(0).ingredientId()).isEqualTo(oreo.getId());
        assertThat(result.ingredients().get(0).matchedName()).isEqualTo("Bánh Oreo");
        assertThat(result.ingredients().get(0).note()).isEqualTo("đập nhuyễn");
        assertThat(result.ingredients().get(1).ingredientId()).isEqualTo(butter.getId());
        assertThat(result.ingredients().get(2).ingredientId()).isNull();
        assertThat(result.ingredients().get(3).ingredientId()).isNull();

        assertThat(result.tools()).extracting(RecipeVideoExtractionResponse.Tool::toolId)
                .containsExactly(whisk.getId(), null);
        assertThat(result.steps().getFirst().ingredients())
                .containsExactly(new RecipeVideoExtractionResponse.ItemRef("i1", BigDecimal.ONE));
        verify(categoryRepository, never()).save(any());
    }

    @Test
    void unknownCategoryFallsBackToOtherAndCreatesItWhenAllowed() {
        when(categoryRepository.findAll()).thenReturn(List.of(cake));
        RecipeCategory other = RecipeCategory.builder().id(UUID.randomUUID()).name("Khác").build();
        when(categoryRepository.save(any())).thenReturn(other);

        RecipeVideoExtractionResponse saved = matcher.match(UUID.randomUUID(), recipe("Bánh mì"), true);
        assertThat(saved.category()).isEqualTo(new RecipeVideoExtractionResponse.Category("Bánh mì", other.getId(), false, "Khác"));

        RecipeVideoExtractionResponse preview = matcher.match(UUID.randomUUID(), recipe("Bánh mì"), false);
        assertThat(preview.category().categoryId()).isNull();
        assertThat(preview.category().matched()).isFalse();
    }

    @Test
    void existingOtherCategoryIsReusedIgnoringDiacritics() {
        RecipeCategory other = RecipeCategory.builder().id(UUID.randomUUID()).name("khac").build();
        when(categoryRepository.findAll()).thenReturn(List.of(cake, other));

        RecipeVideoExtractionResponse result = matcher.match(UUID.randomUUID(), recipe(null), true);

        assertThat(result.category().categoryId()).isEqualTo(other.getId());
        verify(categoryRepository, never()).save(any());
    }

    private static ExtractedRecipe recipe(String categoryName) {
        return new ExtractedRecipe(true, categoryName, "Bánh", null, null, null, null, null,
                List.of(new ExtractedRecipe.Tool("t1", "phới lồng", null, null),
                        new ExtractedRecipe.Tool("t2", "Khuôn tim", BigDecimal.ONE, null)),
                List.of(new ExtractedRecipe.Ingredient("i1", "bánh oreo", "gói", BigDecimal.ONE, "đập nhuyễn"),
                        new ExtractedRecipe.Ingredient("i2", "Unsalted Butter", "g", null, null),
                        new ExtractedRecipe.Ingredient("i3", "sốt chocolate", null, null, null),
                        new ExtractedRecipe.Ingredient("i4", "Bánh  OREO", null, null, null)),
                List.of(new ExtractedRecipe.Step(1, "Đập bánh", null, null, RecipeStepType.PREPARATION,
                        List.of(), List.of(new ExtractedRecipe.ItemRef("i1", BigDecimal.ONE)))));
    }
}
