package com.manhnv.capstonebakerai.recipe.helper;

import com.manhnv.capstonebakerai.recipe.dto.request.RecipeStepEmbeddedRequest;
import com.manhnv.capstonebakerai.recipe.entity.Recipe;
import com.manhnv.capstonebakerai.recipe.entity.RecipeStep;
import com.manhnv.capstonebakerai.recipe.enums.RecipeStepType;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.Objects;
import java.util.function.Function;

@Component
public class RecipeStepDurationHelper {

    public void validateEmbeddedSteps(Integer prepTimeMinutes, Integer cookTimeMinutes,
                                      Collection<RecipeStepEmbeddedRequest> steps) {
        if (steps == null || steps.isEmpty()) {
            return;
        }
        int prepSum = sum(steps, RecipeStepType.PREPARATION,
                RecipeStepEmbeddedRequest::getRecipeStepType, RecipeStepEmbeddedRequest::getDurationMinutes);
        int cookSum = sum(steps, RecipeStepType.COOKING,
                RecipeStepEmbeddedRequest::getRecipeStepType, RecipeStepEmbeddedRequest::getDurationMinutes);

        if (!Objects.equals(prepTimeMinutes, prepSum)) {
            throw new RuntimeException("Total durationMinutes of PREPARATION steps (" + prepSum
                    + ") must equal prepTimeMinutes (" + prepTimeMinutes + ")");
        }
        if (!Objects.equals(cookTimeMinutes, cookSum)) {
            throw new RuntimeException("Total durationMinutes of COOKING steps (" + cookSum
                    + ") must equal cookTimeMinutes (" + cookTimeMinutes + ")");
        }
    }

    public void syncRecipeTimes(Recipe recipe, Collection<RecipeStep> steps) {
        recipe.setPrepTimeMinutes(sum(steps, RecipeStepType.PREPARATION,
                RecipeStep::getRecipeStepType, RecipeStep::getDurationMinutes));
        recipe.setCookTimeMinutes(sum(steps, RecipeStepType.COOKING,
                RecipeStep::getRecipeStepType, RecipeStep::getDurationMinutes));
    }

    private static <T> int sum(Collection<T> steps, RecipeStepType type,
                               Function<T, RecipeStepType> typeOf, Function<T, Integer> durationOf) {
        return steps.stream()
                .filter(step -> typeOf.apply(step) == type)
                .map(durationOf)
                .filter(Objects::nonNull)
                .mapToInt(Integer::intValue)
                .sum();
    }
}
