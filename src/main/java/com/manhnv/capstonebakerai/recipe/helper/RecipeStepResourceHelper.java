package com.manhnv.capstonebakerai.recipe.helper;

import com.manhnv.capstonebakerai.recipe.dto.request.IngredientEmbeddedRequest;
import com.manhnv.capstonebakerai.recipe.dto.request.RecipeStepEmbeddedRequest;
import com.manhnv.capstonebakerai.recipe.dto.request.ToolEmbeddedRequest;
import com.manhnv.capstonebakerai.recipe.entity.RecipeStep;
import com.manhnv.capstonebakerai.recipe.entity.RecipeStepIngredient;
import com.manhnv.capstonebakerai.recipe.entity.RecipeStepTool;
import com.manhnv.capstonebakerai.recipe.repository.IngredientRepository;
import com.manhnv.capstonebakerai.recipe.repository.RecipeIngredientRepository;
import com.manhnv.capstonebakerai.recipe.repository.RecipeToolRepository;
import com.manhnv.capstonebakerai.recipe.repository.ToolRepository;
import com.manhnv.capstonebakerai.recipe.repository.RecipeStepIngredientRepository;
import com.manhnv.capstonebakerai.recipe.repository.RecipeStepToolRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;

@Component
@RequiredArgsConstructor
public class RecipeStepResourceHelper {
    private final IngredientRepository ingredientRepository;
    private final ToolRepository toolRepository;
    private final RecipeStepIngredientRepository stepIngredientRepository;
    private final RecipeStepToolRepository stepToolRepository;
    private final RecipeIngredientRepository recipeIngredientRepository;
    private final RecipeToolRepository recipeToolRepository;

    /** Full replacement, matching the PUT semantics of recipe and step images. */
    @Transactional
    public void replaceResources(RecipeStep step, List<IngredientEmbeddedRequest> ingredients,
                                 List<ToolEmbeddedRequest> tools) {
        List<IngredientEmbeddedRequest> requestedIngredients = ingredients == null ? List.of() : ingredients;
        List<ToolEmbeddedRequest> requestedTools = tools == null ? List.of() : tools;
        validateUnique(requestedIngredients.stream().map(IngredientEmbeddedRequest::getIngredientId).toList(), "ingredient");
        validateUnique(requestedTools.stream().map(ToolEmbeddedRequest::getToolId).toList(), "tool");

        // Resolve both lists before replacing existing links. The caller's transaction covers all changes.
        List<RecipeStepIngredient> ingredientLinks = requestedIngredients.stream().map(request ->
                RecipeStepIngredient.builder().recipeStep(step)
                        .ingredient(ingredientRepository.findById(request.getIngredientId())
                                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Ingredient not found")))
                        .quantity(request.getQuantity()).build()).toList();
        List<RecipeStepTool> toolLinks = requestedTools.stream().map(request ->
                RecipeStepTool.builder().recipeStep(step)
                        .tool(toolRepository.findById(request.getToolId())
                                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Tool not found")))
                        .quantity(request.getQuantity()).build()).toList();

        stepIngredientRepository.deleteByRecipeStep_Id(step.getId());
        stepToolRepository.deleteByRecipeStep_Id(step.getId());
        // Execute deletes before inserting the same step/resource pair under the unique constraints.
        stepIngredientRepository.flush();
        stepToolRepository.flush();
        step.setIngredients(stepIngredientRepository.saveAll(ingredientLinks));
        step.setTools(stepToolRepository.saveAll(toolLinks));
    }

    public void validateEmbeddedRecipe(List<IngredientEmbeddedRequest> recipeIngredients,
                                       List<ToolEmbeddedRequest> recipeTools,
                                       List<RecipeStepEmbeddedRequest> steps) {
        List<IngredientEmbeddedRequest> ingredients = recipeIngredients == null ? List.of() : recipeIngredients;
        List<ToolEmbeddedRequest> tools = recipeTools == null ? List.of() : recipeTools;
        List<RecipeStepEmbeddedRequest> stepList = steps == null ? List.of() : steps;

        Map<UUID, BigDecimal> recipeIngredientQty = toQuantityMap(ingredients,
                IngredientEmbeddedRequest::getIngredientId, IngredientEmbeddedRequest::getQuantity, "ingredient", "recipe");
        Map<UUID, BigDecimal> recipeToolQty = toQuantityMap(tools,
                ToolEmbeddedRequest::getToolId, ToolEmbeddedRequest::getQuantity, "tool", "recipe");
        if (stepList.isEmpty()) {
            return;
        }

        Map<UUID, BigDecimal> usedIngredientQty = new HashMap<>();
        for (RecipeStepEmbeddedRequest step : stepList) {
            for (IngredientEmbeddedRequest ingredient : nullToEmpty(step.getIngredients())) {
                requireInRecipe(recipeIngredientQty, ingredient.getIngredientId(), "Ingredient", step.getStepNumber());
                usedIngredientQty.merge(ingredient.getIngredientId(), qty(ingredient.getQuantity()), BigDecimal::add);
            }
            for (ToolEmbeddedRequest tool : nullToEmpty(step.getTools())) {
                requireInRecipe(recipeToolQty, tool.getToolId(), "Tool", step.getStepNumber());
                requireToolWithinRecipe(recipeToolQty, tool, step.getStepNumber());
            }
        }

        recipeIngredientQty.forEach((ingredientId, recipeQty) -> {
            BigDecimal used = usedIngredientQty.getOrDefault(ingredientId, BigDecimal.ZERO);
            if (used.compareTo(recipeQty) != 0) {
                throw badRequest("Total quantity of ingredient " + ingredientId + " across steps (" + used.toPlainString()
                        + ") must equal the recipe quantity (" + recipeQty.toPlainString() + ")");
            }
        });
    }

    @Transactional(readOnly = true)
    public void validateStepAgainstRecipe(UUID recipeId, UUID stepId, Integer stepNumber,
                                          List<IngredientEmbeddedRequest> ingredients,
                                          List<ToolEmbeddedRequest> tools) {
        Map<UUID, BigDecimal> recipeIngredientQty = new HashMap<>();
        recipeIngredientRepository.findByRecipe_Id(recipeId)
                .forEach(link -> recipeIngredientQty.put(link.getIngredient().getId(), qty(link.getQuantity())));
        Map<UUID, BigDecimal> recipeToolQty = new HashMap<>();
        recipeToolRepository.findByRecipe_Id(recipeId)
                .forEach(link -> recipeToolQty.put(link.getTool().getId(), qty(link.getQuantity())));

        Map<UUID, BigDecimal> usedIngredientQty = new HashMap<>();
        stepIngredientRepository.findByRecipeStep_Recipe_Id(recipeId).stream()
                .filter(link -> stepId == null || !stepId.equals(link.getRecipeStep().getId()))
                .forEach(link -> usedIngredientQty.merge(link.getIngredient().getId(), qty(link.getQuantity()), BigDecimal::add));

        for (IngredientEmbeddedRequest ingredient : nullToEmpty(ingredients)) {
            requireInRecipe(recipeIngredientQty, ingredient.getIngredientId(), "Ingredient", stepNumber);
            BigDecimal total = usedIngredientQty.getOrDefault(ingredient.getIngredientId(), BigDecimal.ZERO)
                    .add(qty(ingredient.getQuantity()));
            BigDecimal recipeQty = recipeIngredientQty.get(ingredient.getIngredientId());
            if (total.compareTo(recipeQty) > 0) {
                throw badRequest("Total quantity of ingredient " + ingredient.getIngredientId() + " across steps ("
                        + total.toPlainString() + ") exceeds the recipe quantity (" + recipeQty.toPlainString() + ")");
            }
        }
        for (ToolEmbeddedRequest tool : nullToEmpty(tools)) {
            requireInRecipe(recipeToolQty, tool.getToolId(), "Tool", stepNumber);
            requireToolWithinRecipe(recipeToolQty, tool, stepNumber);
        }
    }

    @Transactional(readOnly = true)
    public void requireIngredientUnusedBySteps(UUID recipeId, UUID ingredientId) {
        if (stepIngredientRepository.findByRecipeStep_Recipe_Id(recipeId).stream()
                .anyMatch(link -> link.getIngredient().getId().equals(ingredientId))) {
            throw badRequest("Ingredient " + ingredientId + " is still used by steps of this recipe");
        }
    }

    @Transactional(readOnly = true)
    public void requireIngredientQuantityCoversSteps(UUID recipeId, UUID ingredientId, BigDecimal recipeQuantity) {
        BigDecimal used = stepIngredientRepository.findByRecipeStep_Recipe_Id(recipeId).stream()
                .filter(link -> link.getIngredient().getId().equals(ingredientId))
                .map(link -> qty(link.getQuantity()))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        if (qty(recipeQuantity).compareTo(used) < 0) {
            throw badRequest("Recipe quantity of ingredient " + ingredientId + " (" + qty(recipeQuantity).toPlainString()
                    + ") is less than the total used by steps (" + used.toPlainString() + ")");
        }
    }

    @Transactional(readOnly = true)
    public void requireToolUnusedBySteps(UUID recipeId, UUID toolId) {
        if (stepToolRepository.findByRecipeStep_Recipe_Id(recipeId).stream()
                .anyMatch(link -> link.getTool().getId().equals(toolId))) {
            throw badRequest("Tool " + toolId + " is still used by steps of this recipe");
        }
    }

    @Transactional(readOnly = true)
    public void requireToolQuantityCoversSteps(UUID recipeId, UUID toolId, BigDecimal recipeQuantity) {
        BigDecimal maxUsed = stepToolRepository.findByRecipeStep_Recipe_Id(recipeId).stream()
                .filter(link -> link.getTool().getId().equals(toolId))
                .map(link -> qty(link.getQuantity()))
                .max(BigDecimal::compareTo)
                .orElse(BigDecimal.ZERO);
        if (qty(recipeQuantity).compareTo(maxUsed) < 0) {
            throw badRequest("Recipe quantity of tool " + toolId + " (" + qty(recipeQuantity).toPlainString()
                    + ") is less than the quantity used in a step (" + maxUsed.toPlainString() + ")");
        }
    }

    private static <T> Map<UUID, BigDecimal> toQuantityMap(List<T> items, Function<T, UUID> idOf,
                                                          Function<T, BigDecimal> qtyOf, String resource, String scope) {
        Map<UUID, BigDecimal> result = new HashMap<>();
        for (T item : items) {
            if (result.put(idOf.apply(item), qty(qtyOf.apply(item))) != null) {
                throw badRequest("Duplicate " + resource + " IDs in " + scope);
            }
        }
        return result;
    }

    private static void requireInRecipe(Map<UUID, BigDecimal> recipeQty, UUID id, String resource, Integer stepNumber) {
        if (!recipeQty.containsKey(id)) {
            throw badRequest(resource + " " + id + " in step " + stepNumber + " is not part of the recipe");
        }
    }

    private static void requireToolWithinRecipe(Map<UUID, BigDecimal> recipeToolQty, ToolEmbeddedRequest tool, Integer stepNumber) {
        BigDecimal recipeQty = recipeToolQty.get(tool.getToolId());
        if (qty(tool.getQuantity()).compareTo(recipeQty) > 0) {
            throw badRequest("Quantity of tool " + tool.getToolId() + " in step " + stepNumber + " ("
                    + qty(tool.getQuantity()).toPlainString() + ") exceeds the recipe quantity (" + recipeQty.toPlainString() + ")");
        }
    }

    private static BigDecimal qty(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }

    private static <T> List<T> nullToEmpty(List<T> list) {
        return list == null ? List.of() : list;
    }

    private static ResponseStatusException badRequest(String message) {
        return new ResponseStatusException(HttpStatus.BAD_REQUEST, message);
    }

    private void validateUnique(List<UUID> ids, String resource) {
        if (new HashSet<>(ids).size() != ids.size()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Duplicate " + resource + " IDs in recipe step");
        }
    }
}
