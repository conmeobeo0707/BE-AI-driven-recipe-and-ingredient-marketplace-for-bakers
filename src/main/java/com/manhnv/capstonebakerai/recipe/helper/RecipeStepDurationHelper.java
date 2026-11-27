package com.manhnv.capstonebakerai.recipe.helper;

import com.manhnv.capstonebakerai.common.exception.AppException;
import com.manhnv.capstonebakerai.recipe.dto.request.RecipeStepEmbeddedRequest;
import com.manhnv.capstonebakerai.recipe.entity.Recipe;
import com.manhnv.capstonebakerai.recipe.entity.RecipeStep;
import com.manhnv.capstonebakerai.recipe.enums.RecipeStepType;
import com.manhnv.capstonebakerai.recipe.exception.RecipeErrorCode;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;

@Component
public class RecipeStepDurationHelper {

    public void validateEmbeddedSteps(Integer prepTimeMinutes, Integer cookTimeMinutes,
                                      Collection<RecipeStepEmbeddedRequest> steps) {
        if (steps == null || steps.isEmpty()) {
            return;
        }
        int prepSum = zeroIfNull(sum(steps, RecipeStepType.PREPARATION,
                RecipeStepEmbeddedRequest::getRecipeStepType, RecipeStepEmbeddedRequest::getDurationMinutes));
        int cookSum = zeroIfNull(sum(steps, RecipeStepType.COOKING,
                RecipeStepEmbeddedRequest::getRecipeStepType, RecipeStepEmbeddedRequest::getDurationMinutes));

        if (zeroIfNull(prepTimeMinutes) != prepSum) {
            throw mismatch(RecipeStepType.PREPARATION, "prepTimeMinutes", prepSum, prepTimeMinutes);
        }
        if (zeroIfNull(cookTimeMinutes) != cookSum) {
            throw mismatch(RecipeStepType.COOKING, "cookTimeMinutes", cookSum, cookTimeMinutes);
        }
    }

    private static AppException mismatch(RecipeStepType type, String recipeField, int stepSum, Integer recipeTime) {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("recipeStepType", type);
        data.put("stepDurationTotal", stepSum);
        data.put(recipeField, recipeTime);
        return new AppException(RecipeErrorCode.STEP_DURATION_MISMATCH,
                "Total durationMinutes of " + type + " steps (" + stepSum + ") must equal "
                        + recipeField + " (" + recipeTime + ")", data);
    }

    public void syncRecipeTimes(Recipe recipe, Collection<RecipeStep> steps) {
        recipe.setPrepTimeMinutes(sum(steps, RecipeStepType.PREPARATION,
                RecipeStep::getRecipeStepType, RecipeStep::getDurationMinutes));
        recipe.setCookTimeMinutes(sum(steps, RecipeStepType.COOKING,
                RecipeStep::getRecipeStepType, RecipeStep::getDurationMinutes));
    }

    private static <T> Integer sum(Collection<T> steps, RecipeStepType type,
                                   Function<T, RecipeStepType> typeOf, Function<T, Integer> durationOf) {
        List<Integer> durations = steps.stream()
                .filter(step -> typeOf.apply(step) == type)
                .map(durationOf)
                .filter(Objects::nonNull)
                .toList();
        return durations.isEmpty() ? null : durations.stream().mapToInt(Integer::intValue).sum();
    }

    private static int zeroIfNull(Integer value) {
        return value == null ? 0 : value;
    }
}
