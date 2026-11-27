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
import com.manhnv.capstonebakerai.common.exception.AppException;
import com.manhnv.capstonebakerai.recipe.exception.RecipeErrorCode;
import com.manhnv.capstonebakerai.recipe.exception.RecipeStepErrors;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

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
                                "ingredientId", request.getIngredientId(), step.getStepNumber()))
                        .quantity(request.getQuantity()).note(trimToNull(request.getNote())).build()).toList();
        List<RecipeStepTool> toolLinks = requestedTools.stream().map(request ->
                RecipeStepTool.builder().recipeStep(step)
                        .recipeTool(requirePresent(recipeToolsByToolId.get(request.getToolId()),
                                "toolId", request.getToolId(), step.getStepNumber()))
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
                .orElseThrow(() -> new AppException(RecipeErrorCode.RECIPE_INGREDIENT_NOT_FOUND));
        if (!sameRecipe(step, link.getRecipe().getId())) {
            throw RecipeStepErrors.notInRecipe("recipeIngredientId", recipeIngredientId, step.getStepNumber());
        }
        return link;
    }

    public RecipeTool findRecipeTool(RecipeStep step, UUID recipeToolId) {
        RecipeTool link = recipeToolRepository.findById(recipeToolId)
                .orElseThrow(() -> new AppException(RecipeErrorCode.RECIPE_TOOL_NOT_FOUND));
        if (!sameRecipe(step, link.getRecipe().getId())) {
            throw RecipeStepErrors.notInRecipe("recipeToolId", recipeToolId, step.getStepNumber());
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
                requireInRecipe(recipeIngredientQty, ingredient.getIngredientId(), "ingredientId", step.getStepNumber());
                usedIngredientQty.merge(ingredient.getIngredientId(), qty(ingredient.getQuantity()), BigDecimal::add);
            }
            for (ToolEmbeddedRequest tool : nullToEmpty(step.getTools())) {
                requireInRecipe(recipeToolQty, tool.getToolId(), "toolId", step.getStepNumber());
                requireToolWithinRecipe(recipeToolQty, "toolId", tool.getToolId(), tool.getQuantity(), step.getStepNumber());
            }
        }

        recipeIngredientQty.forEach((ingredientId, recipeQty) -> {
            BigDecimal used = usedIngredientQty.getOrDefault(ingredientId, BigDecimal.ZERO);
            if (used.compareTo(recipeQty) != 0) {
                throw RecipeStepErrors.ingredientTotalMismatch(ingredientId, used, recipeQty);
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
            requireInRecipe(recipeIngredientQty, id, "recipeIngredientId", stepNumber);
            BigDecimal total = usedIngredientQty.getOrDefault(id, BigDecimal.ZERO).add(qty(ingredient.getQuantity()));
            BigDecimal recipeQty = recipeIngredientQty.get(id);
            if (total.compareTo(recipeQty) > 0) {
                throw RecipeStepErrors.ingredientTotalExceeds("recipeIngredientId", id, stepNumber, total, recipeQty);
            }
        }
        for (StepToolEmbeddedRequest tool : nullToEmpty(tools)) {
            requireInRecipe(recipeToolQty, tool.getRecipeToolId(), "recipeToolId", stepNumber);
            requireToolWithinRecipe(recipeToolQty, "recipeToolId", tool.getRecipeToolId(), tool.getQuantity(), stepNumber);
        }
    }

    @Transactional(readOnly = true)
    public void requireRecipeIngredientUnusedBySteps(UUID recipeIngredientId) {
        if (stepIngredientRepository.existsByRecipeIngredient_Id(recipeIngredientId)) {
            throw RecipeStepErrors.usedBySteps("recipeIngredientId", recipeIngredientId);
        }
    }

    @Transactional(readOnly = true)
    public void requireRecipeIngredientQuantityCoversSteps(UUID recipeIngredientId, BigDecimal recipeQuantity) {
        BigDecimal used = stepIngredientRepository.findByRecipeIngredient_Id(recipeIngredientId).stream()
                .map(link -> qty(link.getQuantity()))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        if (qty(recipeQuantity).compareTo(used) < 0) {
            throw RecipeStepErrors.quantityBelowStepUsage("recipeIngredientId", recipeIngredientId, qty(recipeQuantity), used);
        }
    }

    @Transactional(readOnly = true)
    public void requireRecipeToolUnusedBySteps(UUID recipeToolId) {
        if (stepToolRepository.existsByRecipeTool_Id(recipeToolId)) {
            throw RecipeStepErrors.usedBySteps("recipeToolId", recipeToolId);
        }
    }

    @Transactional(readOnly = true)
    public void requireRecipeToolQuantityCoversSteps(UUID recipeToolId, BigDecimal recipeQuantity) {
        BigDecimal maxUsed = stepToolRepository.findByRecipeTool_Id(recipeToolId).stream()
                .map(link -> qty(link.getQuantity()))
                .max(BigDecimal::compareTo)
                .orElse(BigDecimal.ZERO);
        if (qty(recipeQuantity).compareTo(maxUsed) < 0) {
            throw RecipeStepErrors.quantityBelowStepUsage("recipeToolId", recipeToolId, qty(recipeQuantity), maxUsed);
        }
    }

    private static boolean sameRecipe(RecipeStep step, UUID recipeId) {
        return step.getRecipe() != null && recipeId != null && recipeId.equals(step.getRecipe().getId());
    }

    private static <T> T requirePresent(T value, String idField, UUID id, Integer stepNumber) {
        if (value == null) {
            throw RecipeStepErrors.notInRecipe(idField, id, stepNumber);
        }
        return value;
    }

    private static <T> Map<UUID, BigDecimal> toQuantityMap(List<T> items, Function<T, UUID> idOf,
                                                          Function<T, BigDecimal> qtyOf, String resource, String scope) {
        Map<UUID, BigDecimal> result = new HashMap<>();
        for (T item : items) {
            if (result.put(idOf.apply(item), qty(qtyOf.apply(item))) != null) {
                throw RecipeStepErrors.duplicateIds(resource, scope);
            }
        }
        return result;
    }

    private static void requireInRecipe(Map<UUID, BigDecimal> recipeQty, UUID id, String idField, Integer stepNumber) {
        if (!recipeQty.containsKey(id)) {
            throw RecipeStepErrors.notInRecipe(idField, id, stepNumber);
        }
    }

    private static void requireToolWithinRecipe(Map<UUID, BigDecimal> recipeToolQty, String idField, UUID toolKey,
                                                BigDecimal quantity, Integer stepNumber) {
        BigDecimal recipeQty = recipeToolQty.get(toolKey);
        if (qty(quantity).compareTo(recipeQty) > 0) {
            throw RecipeStepErrors.toolQuantityExceeds(idField, toolKey, stepNumber, qty(quantity), recipeQty);
        }
    }

    private static BigDecimal qty(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }

    private static <T> List<T> nullToEmpty(List<T> list) {
        return list == null ? List.of() : list;
    }

    private static void validateUnique(List<UUID> ids, String resource) {
        if (new HashSet<>(ids).size() != ids.size()) {
            throw RecipeStepErrors.duplicateIds(resource, "recipe step");
        }
    }
}
