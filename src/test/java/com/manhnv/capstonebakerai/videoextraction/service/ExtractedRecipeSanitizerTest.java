package com.manhnv.capstonebakerai.videoextraction.service;

import com.manhnv.capstonebakerai.recipe.enums.DifficultyEnum;
import com.manhnv.capstonebakerai.recipe.enums.RecipeStepType;
import com.manhnv.capstonebakerai.videoextraction.client.RawExtractedRecipe;
import com.manhnv.capstonebakerai.videoextraction.client.RawExtractedRecipe.Ingredient;
import com.manhnv.capstonebakerai.videoextraction.client.RawExtractedRecipe.ItemRef;
import com.manhnv.capstonebakerai.videoextraction.client.RawExtractedRecipe.Step;
import com.manhnv.capstonebakerai.videoextraction.client.RawExtractedRecipe.Tool;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class ExtractedRecipeSanitizerTest {

    private final ExtractedRecipeSanitizer sanitizer = new ExtractedRecipeSanitizer();

    @Test
    void notRecipeKeepsEveryFieldNullOrEmpty() {
        RawExtractedRecipe raw = new RawExtractedRecipe(false, "Bánh", "x", null, "EASY", 1, 2, 3,
                List.of(new Tool("t1", "Lò", null, null)), List.of(), List.of());

        ExtractedRecipe result = sanitizer.sanitize(raw);

        assertThat(result.isRecipe()).isFalse();
        assertThat(result.title()).isNull();
        assertThat(result.categoryName()).isNull();
        assertThat(result.tools()).isEmpty();
        assertThat(result.steps()).isEmpty();
    }

    @Test
    void stepsReferenceOnlyKnownKeysAndAreRenumbered() {
        RawExtractedRecipe raw = new RawExtractedRecipe(true, " Bánh mì ", "  Bánh mì chảo ", "Ngon", "medium",
                99, 99, 0,
                List.of(new Tool("t1", "Chảo", BigDecimal.ONE, " chống dính "), new Tool("t1", "Tô", null, null),
                        new Tool("t3", " ", null, null)),
                List.of(new Ingredient("i1", "Hành tím", "g", new BigDecimal("50"), "băm nhỏ"),
                        new Ingredient(null, "Trứng", "quả", new BigDecimal("-2"), "  ")),
                List.of(
                        new Step(5, "Chiên trứng", 10, null, "COOKING",
                                List.of(new ItemRef("t1", null), new ItemRef("t9", null)),
                                List.of(new ItemRef("i1", new BigDecimal("20")), new ItemRef("i1", null),
                                        new ItemRef("missing", null))),
                        new Step(2, "Băm hành", 5, "Băm nhỏ", "PREPARATION", List.of(), null),
                        new Step(3, "  ", 7, null, "COOKING", null, null),
                        new Step(4, "Trộn", null, null, "unknown", null, null)));

        ExtractedRecipe result = sanitizer.sanitize(raw);

        assertThat(result.categoryName()).isEqualTo("Bánh mì");
        assertThat(result.title()).isEqualTo("Bánh mì chảo");
        assertThat(result.difficulty()).isEqualTo(DifficultyEnum.MEDIUM);
        assertThat(result.servings()).isNull();
        assertThat(result.tools()).extracting(ExtractedRecipe.Tool::key).containsExactly("t1", "t2");
        assertThat(result.tools().getFirst().note()).isEqualTo("chống dính");
        assertThat(result.ingredients()).extracting(ExtractedRecipe.Ingredient::key).containsExactly("i1", "i2");
        assertThat(result.ingredients().get(0).note()).isEqualTo("băm nhỏ");
        assertThat(result.ingredients().get(1).quantity()).isNull();
        assertThat(result.ingredients().get(1).note()).isNull();

        assertThat(result.steps()).extracting(ExtractedRecipe.Step::stepNumber).containsExactly(1, 2, 3);
        assertThat(result.steps()).extracting(ExtractedRecipe.Step::instruction)
                .containsExactly("Băm hành", "Trộn", "Chiên trứng");
        assertThat(result.steps().get(1).recipeStepType()).isEqualTo(RecipeStepType.COOKING);

        ExtractedRecipe.Step fry = result.steps().get(2);
        assertThat(fry.tools()).extracting(ExtractedRecipe.ItemRef::key).containsExactly("t1");
        assertThat(fry.ingredients()).containsExactly(new ExtractedRecipe.ItemRef("i1", new BigDecimal("20")));

        assertThat(result.prepTimeMinutes()).isEqualTo(5);
        assertThat(result.cookTimeMinutes()).isEqualTo(10);
    }

    @Test
    void ingredientQuantityCoversWhatStepsUse() {
        RawExtractedRecipe raw = new RawExtractedRecipe(true, null, "Món", null, null, null, null, null,
                List.of(),
                List.of(new Ingredient("i1", "Bột mì", "g", null, null),
                        new Ingredient("i2", "Đường", "g", new BigDecimal("10"), null),
                        new Ingredient("i3", "Muối", "g", new BigDecimal("5"), null)),
                List.of(
                        new Step(1, "Trộn", null, null, "PREPARATION", null,
                                List.of(new ItemRef("i1", new BigDecimal("100")), new ItemRef("i2", new BigDecimal("8")),
                                        new ItemRef("i3", new BigDecimal("2")))),
                        new Step(2, "Thêm", null, null, "PREPARATION", null,
                                List.of(new ItemRef("i1", new BigDecimal("50")), new ItemRef("i2", new BigDecimal("8"))))));

        ExtractedRecipe result = sanitizer.sanitize(raw);

        assertThat(result.ingredients()).extracting(ExtractedRecipe.Ingredient::quantity)
                .containsExactly(new BigDecimal("150"), new BigDecimal("16"), new BigDecimal("5"));
    }

    @Test
    void timesAreNullWhenNoStepOfThatTypeHasDuration() {
        RawExtractedRecipe raw = new RawExtractedRecipe(true, null, "Món", null, null, 15, 30, 2,
                List.of(), List.of(),
                List.of(new Step(1, "Nướng", null, null, "COOKING", null, null)));

        ExtractedRecipe result = sanitizer.sanitize(raw);

        assertThat(result.prepTimeMinutes()).isNull();
        assertThat(result.cookTimeMinutes()).isNull();
        assertThat(result.servings()).isEqualTo(2);
    }
}
