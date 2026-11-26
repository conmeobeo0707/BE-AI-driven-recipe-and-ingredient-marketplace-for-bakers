package com.manhnv.capstonebakerai.recipe.helper;

import com.manhnv.capstonebakerai.recipe.dto.request.IngredientEmbeddedRequest;
import com.manhnv.capstonebakerai.recipe.dto.request.RecipeStepEmbeddedRequest;
import com.manhnv.capstonebakerai.recipe.dto.request.StepIngredientEmbeddedRequest;
import com.manhnv.capstonebakerai.recipe.dto.request.StepToolEmbeddedRequest;
import com.manhnv.capstonebakerai.recipe.dto.request.ToolEmbeddedRequest;
import com.manhnv.capstonebakerai.recipe.entity.RecipeIngredient;
import com.manhnv.capstonebakerai.recipe.entity.RecipeStep;
import com.manhnv.capstonebakerai.recipe.entity.RecipeStepIngredient;
import com.manhnv.capstonebakerai.recipe.entity.RecipeStepTool;
import com.manhnv.capstonebakerai.recipe.entity.RecipeTool;
import com.manhnv.capstonebakerai.recipe.repository.RecipeIngredientRepository;
import com.manhnv.capstonebakerai.recipe.repository.RecipeStepIngredientRepository;
import com.manhnv.capstonebakerai.recipe.repository.RecipeStepToolRepository;
import com.manhnv.capstonebakerai.recipe.repository.RecipeToolRepository;
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
    private final RecipeStepIngredientRepository stepIngredientRepository;
    private final RecipeStepToolRepository stepToolRepository;
    private final RecipeIngredientRepository recipeIngredientRepository;
    private final RecipeToolRepository recipeToolRepository;

    @Transactional
    public void replaceResources(RecipeStep step, List<StepIngredientEmbeddedRequest> ingredients,
                                 List<StepToolEmbeddedRequest> tools) {
        List<StepIngredientEmbeddedRequest> requestedIngredients = nullToEmpty(ingredients);
        List<StepToolEmbeddedRequest> requestedTools = nullToEmpty(tools);
        validateUnique(requestedIngredients.stream().map(StepIngredientEmbeddedRequest::getRecipeIngredientId).toList(), "recipe ingredient");
        validateUnique(requestedTools.stream().map(StepToolEmbeddedRequest::getRecipeToolId).toList(), "recipe tool");

        List<RecipeStepIngredient> ingredientLinks = requestedIngredients.stream().map(request ->
                RecipeStepIngredient.builder().recipeStep(step)
                        .recipeIngredient(findRecipeIngredient(step, request.getRecipeIngredientId()))
                        .quantity(request.getQuantity()).note(trimToNull(request.getNote())).build()).toList();
        List<RecipeStepTool> toolLinks = requestedTools.stream().map(request ->
                RecipeStepTool.builder().recipeStep(step)
                        .recipeTool(findRecipeTool(step, request.getRecipeToolId()))
                        .quantity(request.getQuantity()).note(trimToNull(request.getNote())).build()).toList();

        replace(step, ingredientLinks, toolLinks);
    }

    @Transactional
    public void replaceEmbeddedResources(RecipeStep step, List<IngredientEmbeddedRequest> ingredients,
                                         List<ToolEmbeddedRequest> tools,
                                         Map<UUID, RecipeIngredient> recipeIngredientsByIngredientId,
                                         Map<UUID, RecipeTool> recipeToolsByToolId) {
        List<IngredientEmbeddedRequest> requestedIngredients = nullToEmpty(ingredients);
        List<ToolEmbeddedRequest> requestedTools = nullToEmpty(tools);
        validateUnique(requestedIngredients.stream().map(IngredientEmbeddedRequest::getIngredientId).toList(), "ingredient");
        validateUnique(requestedTools.stream().map(ToolEmbeddedRequest::getToolId).toList(), "tool");

        List<RecipeStepIngredient> ingredientLinks = requestedIngredients.stream().map(request ->
                RecipeStepIngredient.builder().recipeStep(step)
                        .recipeIngredient(requirePresent(recipeIngredientsByIngredientId.get(request.getIngredientId()),
                                "Ingredient " + request.getIngredientId() + " in step " + step.getStepNumber()
                                        + " is not part of the recipe"))
                        .quantity(request.getQuantity()).note(trimToNull(request.getNote())).build()).toList();
        List<RecipeStepTool> toolLinks = requestedTools.stream().map(request ->
                RecipeStepTool.builder().recipeStep(step)
                        .recipeTool(requirePresent(recipeToolsByToolId.get(request.getToolId()),
                                "Tool " + request.getToolId() + " in step " + step.getStepNumber()
                                        + " is not part of the recipe"))
                        .quantity(request.getQuantity()).note(trimToNull(request.getNote())).build()).toList();

        replace(step, ingredientLinks, toolLinks);
    }

    private static String trimToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private void replace(RecipeStep step, List<RecipeStepIngredient> ingredientLinks, List<RecipeStepTool> toolLinks) {
        stepIngredientRepository.deleteByRecipeStep_Id(step.getId());
        stepToolRepository.deleteByRecipeStep_Id(step.getId());
        stepIngredientRepository.flush();
        stepToolRepository.flush();
        step.setIngredients(stepIngredientRepository.saveAll(ingredientLinks));
        step.setTools(stepToolRepository.saveAll(toolLinks));
    }

    public RecipeIngredient findRecipeIngredient(RecipeStep step, UUID recipeIngredientId) {
        RecipeIngredient link = recipeIngredientRepository.findById(recipeIngredientId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Recipe ingredient " + recipeIngredientId + " not found"));
        if (!sameRecipe(step, link.getRecipe().getId())) {
            throw badRequest("Recipe ingredient " + recipeIngredientId + " is not part of the step's recipe");
        }
        return link;
    }

    public RecipeTool findRecipeTool(RecipeStep step, UUID recipeToolId) {
        RecipeTool link = recipeToolRepository.findById(recipeToolId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Recipe tool " + recipeToolId + " not found"));
        if (!sameRecipe(step, link.getRecipe().getId())) {
            throw badRequest("Recipe tool " + recipeToolId + " is not part of the step's recipe");
        }
        return link;
    }

    public void validateEmbeddedRecipe(List<IngredientEmbeddedRequest> recipeIngredients,
                                       List<ToolEmbeddedRequest> recipeTools,
                                       List<RecipeStepEmbeddedRequest> steps) {
        List<IngredientEmbeddedRequest> ingredients = nullToEmpty(recipeIngredients);
        List<ToolEmbeddedRequest> tools = nullToEmpty(recipeTools);
        List<RecipeStepEmbeddedRequest> stepList = nullToEmpty(steps);

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
                requireToolWithinRecipe(recipeToolQty, tool.getToolId(), tool.getQuantity(), step.getStepNumber());
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
                                          List<StepIngredientEmbeddedRequest> ingredients,
                                          List<StepToolEmbeddedRequest> tools) {
        Map<UUID, BigDecimal> recipeIngredientQty = new HashMap<>();
        recipeIngredientRepository.findByRecipe_Id(recipeId)
                .forEach(link -> recipeIngredientQty.put(link.getId(), qty(link.getQuantity())));
        Map<UUID, BigDecimal> recipeToolQty = new HashMap<>();
        recipeToolRepository.findByRecipe_Id(recipeId)
                .forEach(link -> recipeToolQty.put(link.getId(), qty(link.getQuantity())));

        Map<UUID, BigDecimal> usedIngredientQty = new HashMap<>();
        stepIngredientRepository.findByRecipeStep_Recipe_Id(recipeId).stream()
                .filter(link -> stepId == null || !stepId.equals(link.getRecipeStep().getId()))
                .forEach(link -> usedIngredientQty.merge(link.getRecipeIngredient().getId(), qty(link.getQuantity()), BigDecimal::add));

        for (StepIngredientEmbeddedRequest ingredient : nullToEmpty(ingredients)) {
            UUID id = ingredient.getRecipeIngredientId();
            requireInRecipe(recipeIngredientQty, id, "Recipe ingredient", stepNumber);
            BigDecimal total = usedIngredientQty.getOrDefault(id, BigDecimal.ZERO).add(qty(ingredient.getQuantity()));
            BigDecimal recipeQty = recipeIngredientQty.get(id);
            if (total.compareTo(recipeQty) > 0) {
                throw badRequest("Total quantity of recipe ingredient " + id + " across steps ("
                        + total.toPlainString() + ") exceeds the recipe quantity (" + recipeQty.toPlainString() + ")");
            }
        }
        for (StepToolEmbeddedRequest tool : nullToEmpty(tools)) {
            requireInRecipe(recipeToolQty, tool.getRecipeToolId(), "Recipe tool", stepNumber);
            requireToolWithinRecipe(recipeToolQty, tool.getRecipeToolId(), tool.getQuantity(), stepNumber);
        }
    }

    @Transactional(readOnly = true)
    public void requireRecipeIngredientUnusedBySteps(UUID recipeIngredientId) {
        if (stepIngredientRepository.existsByRecipeIngredient_Id(recipeIngredientId)) {
            throw badRequest("Recipe ingredient " + recipeIngredientId + " is still used by steps of this recipe");
        }
    }

    @Transactional(readOnly = true)
    public void requireRecipeIngredientQuantityCoversSteps(UUID recipeIngredientId, BigDecimal recipeQuantity) {
        BigDecimal used = stepIngredientRepository.findByRecipeIngredient_Id(recipeIngredientId).stream()
                .map(link -> qty(link.getQuantity()))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        if (qty(recipeQuantity).compareTo(used) < 0) {
            throw badRequest("Recipe quantity of recipe ingredient " + recipeIngredientId + " ("
                    + qty(recipeQuantity).toPlainString() + ") is less than the total used by steps ("
                    + used.toPlainString() + ")");
        }
    }

    @Transactional(readOnly = true)
    public void requireRecipeToolUnusedBySteps(UUID recipeToolId) {
        if (stepToolRepository.existsByRecipeTool_Id(recipeToolId)) {
            throw badRequest("Recipe tool " + recipeToolId + " is still used by steps of this recipe");
        }
    }

    @Transactional(readOnly = true)
    public void requireRecipeToolQuantityCoversSteps(UUID recipeToolId, BigDecimal recipeQuantity) {
        BigDecimal maxUsed = stepToolRepository.findByRecipeTool_Id(recipeToolId).stream()
                .map(link -> qty(link.getQuantity()))
                .max(BigDecimal::compareTo)
                .orElse(BigDecimal.ZERO);
        if (qty(recipeQuantity).compareTo(maxUsed) < 0) {
            throw badRequest("Recipe quantity of recipe tool " + recipeToolId + " ("
                    + qty(recipeQuantity).toPlainString() + ") is less than the quantity used in a step ("
                    + maxUsed.toPlainString() + ")");
        }
    }

    private static boolean sameRecipe(RecipeStep step, UUID recipeId) {
        return step.getRecipe() != null && recipeId != null && recipeId.equals(step.getRecipe().getId());
    }

    private static <T> T requirePresent(T value, String message) {
        if (value == null) {
            throw badRequest(message);
        }
        return value;
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

    private static void requireToolWithinRecipe(Map<UUID, BigDecimal> recipeToolQty, UUID toolKey,
                                                BigDecimal quantity, Integer stepNumber) {
        BigDecimal recipeQty = recipeToolQty.get(toolKey);
        if (qty(quantity).compareTo(recipeQty) > 0) {
            throw badRequest("Quantity of tool " + toolKey + " in step " + stepNumber + " ("
                    + qty(quantity).toPlainString() + ") exceeds the recipe quantity (" + recipeQty.toPlainString() + ")");
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

    private static void validateUnique(List<UUID> ids, String resource) {
        if (new HashSet<>(ids).size() != ids.size()) {
            throw badRequest("Duplicate " + resource + " IDs in recipe step");
        }
    }
}
