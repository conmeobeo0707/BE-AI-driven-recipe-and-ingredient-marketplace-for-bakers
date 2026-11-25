package com.manhnv.capstonebakerai.recipe.helper;

import com.manhnv.capstonebakerai.recipe.dto.request.RecipeStepEmbeddedRequest;
import com.manhnv.capstonebakerai.recipe.entity.Recipe;
import com.manhnv.capstonebakerai.recipe.entity.RecipeStep;
import com.manhnv.capstonebakerai.recipe.enums.RecipeStepType;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class RecipeStepDurationHelperTest {

    private final RecipeStepDurationHelper helper = new RecipeStepDurationHelper();

    private static RecipeStepEmbeddedRequest request(RecipeStepType type, Integer duration) {
        return RecipeStepEmbeddedRequest.builder().stepNumber(1).instruction("x")
                .recipeStepType(type).durationMinutes(duration).build();
    }

    private static RecipeStep step(RecipeStepType type, Integer duration) {
        return RecipeStep.builder().instruction("x").recipeStepType(type).durationMinutes(duration).build();
    }

    @Test
    void nullRecipeTimesAcceptStepsWithoutDurations() {
        assertThatCode(() -> helper.validateEmbeddedSteps(null, null, List.of(
                request(RecipeStepType.PREPARATION, null), request(RecipeStepType.COOKING, null))))
                .doesNotThrowAnyException();
    }

    @Test
    void nullRecipeTimeRejectsStepsThatHaveDurations() {
        assertThatThrownBy(() -> helper.validateEmbeddedSteps(null, null, List.of(
                request(RecipeStepType.PREPARATION, 10))))
                .hasMessageContaining("PREPARATION");
    }

    @Test
    void recipeTimesMustMatchSumOfNonNullDurations() {
        assertThatCode(() -> helper.validateEmbeddedSteps(10, 30, List.of(
                request(RecipeStepType.PREPARATION, 10), request(RecipeStepType.PREPARATION, null),
                request(RecipeStepType.COOKING, 30))))
                .doesNotThrowAnyException();
        assertThatThrownBy(() -> helper.validateEmbeddedSteps(10, 20, List.of(
                request(RecipeStepType.PREPARATION, 10), request(RecipeStepType.COOKING, 30))))
                .hasMessageContaining("COOKING");
    }

    @Test
    void syncSetsNullWhenNoStepOfTypeHasDuration() {
        Recipe recipe = Recipe.builder().prepTimeMinutes(5).cookTimeMinutes(5).build();
        helper.syncRecipeTimes(recipe, List.of(
                step(RecipeStepType.PREPARATION, null), step(RecipeStepType.COOKING, 20),
                step(RecipeStepType.COOKING, 15)));
        assertThat(recipe.getPrepTimeMinutes()).isNull();
        assertThat(recipe.getCookTimeMinutes()).isEqualTo(35);
    }
}
