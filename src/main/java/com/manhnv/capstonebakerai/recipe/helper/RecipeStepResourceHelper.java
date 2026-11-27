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
import com.manhnv.capstonebakerai.recipe.enums.RecipeStatus;
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
import java.util.ArrayList;
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
                                         Map<String, RecipeIngredient> recipeIngredientsByRefKey,
                                         Map<String, RecipeTool> recipeToolsByRefKey) {
        List<IngredientEmbeddedRequest> requestedIngredients = nullToEmpty(ingredients);
        List<ToolEmbeddedRequest> requestedTools = nullToEmpty(tools);
        Map<UUID, String> ingredientKeyById = new HashMap<>();
        recipeIngredientsByRefKey.forEach((key, link) -> {
            if (link.getIngredient() != null) ingredientKeyById.put(link.getIngredient().getId(), key);
        });
        Map<UUID, String> toolKeyById = new HashMap<>();
        recipeToolsByRefKey.forEach((key, link) -> {
            if (link.getTool() != null) toolKeyById.put(link.getTool().getId(), key);
        });

        List<String> ingredientKeys = requestedIngredients.stream()
                .map(request -> stepKey(request.getRef(), request.getIngredientId(), ingredientKeyById)).toList();
        List<String> toolKeys = requestedTools.stream()
                .map(request -> stepKey(request.getRef(), request.getToolId(), toolKeyById)).toList();
        validateUnique(ingredientKeys, "ingredient");
        validateUnique(toolKeys, "tool");

        List<RecipeStepIngredient> ingredientLinks = new ArrayList<>();
        for (int i = 0; i < requestedIngredients.size(); i++) {
            IngredientEmbeddedRequest request = requestedIngredients.get(i);
            RecipeIngredient link = requirePresent(lookup(recipeIngredientsByRefKey, ingredientKeys.get(i)),
                    stepIdField(request.getRef(), "ingredientId"), stepIdValue(request.getRef(), request.getIngredientId()),
                    step.getStepNumber());
            ingredientLinks.add(RecipeStepIngredient.builder().recipeStep(step).recipeIngredient(link)
                    .quantity(request.getQuantity()).note(trimToNull(request.getNote())).build());
        }
        List<RecipeStepTool> toolLinks = new ArrayList<>();
        for (int i = 0; i < requestedTools.size(); i++) {
            ToolEmbeddedRequest request = requestedTools.get(i);
            RecipeTool link = requirePresent(lookup(recipeToolsByRefKey, toolKeys.get(i)),
                    stepIdField(request.getRef(), "toolId"), stepIdValue(request.getRef(), request.getToolId()),
                    step.getStepNumber());
            toolLinks.add(RecipeStepTool.builder().recipeStep(step).recipeTool(link)
                    .quantity(request.getQuantity()).note(trimToNull(request.getNote())).build());
        }

        replace(step, ingredientLinks, toolLinks);
    }

    /**
     * Reference key of a recipe-level row: the client {@code ref} when given, otherwise the catalog id.
     * Step items point at recipe rows through this key, so unmatched rows (id = null) stay distinguishable.
     */
    public static String refKey(IngredientEmbeddedRequest request) {
        return refKey(request.getRef(), request.getIngredientId());
    }

    public static String refKey(ToolEmbeddedRequest request) {
        return refKey(request.getRef(), request.getToolId());
    }

    private static String refKey(String ref, UUID id) {
        if (!isBlank(ref)) {
            return ref.trim();
        }
        return id == null ? null : id.toString();
    }

    /**
     * Step items reference a recipe row by {@code ref}, or — for clients that do not send refs — by catalog id.
     */
    private static String stepKey(String ref, UUID id, Map<UUID, String> recipeKeyById) {
        if (!isBlank(ref)) {
            return ref.trim();
        }
        return id == null ? null : recipeKeyById.get(id);
    }

    private static String stepIdField(String ref, String idField) {
        return isBlank(ref) ? idField : "ref";
    }

    private static Object stepIdValue(String ref, UUID id) {
        return isBlank(ref) ? id : ref.trim();
    }

    private static <T> T lookup(Map<String, T> map, String key) {
        return key == null ? null : map.get(key);
    }

    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    /**
     * Rows without a catalog id are only allowed in PERSONAL recipes, and then need a rawName and a ref.
     */
    public void validateCatalogItems(RecipeStatus recipeStatus,
                                     List<IngredientEmbeddedRequest> recipeIngredients,
                                     List<ToolEmbeddedRequest> recipeTools) {
        validateCatalogItems(recipeStatus, nullToEmpty(recipeIngredients), IngredientEmbeddedRequest::getIngredientId,
                IngredientEmbeddedRequest::getRawName, IngredientEmbeddedRequest::getRef, "ingredient");
        validateCatalogItems(recipeStatus, nullToEmpty(recipeTools), ToolEmbeddedRequest::getToolId,
                ToolEmbeddedRequest::getRawName, ToolEmbeddedRequest::getRef, "tool");
    }

    private static <T> void validateCatalogItems(RecipeStatus recipeStatus, List<T> items, Function<T, UUID> idOf,
                                                 Function<T, String> rawNameOf, Function<T, String> refOf,
                                                 String itemType) {
        for (int i = 0; i < items.size(); i++) {
            T item = items.get(i);
            if (item == null || idOf.apply(item) != null) {
                continue;
            }
            if (recipeStatus != RecipeStatus.PERSONAL) {
                throw RecipeStepErrors.unmatchedItemNotAllowed(itemType, i, rawNameOf.apply(item));
            }
            if (isBlank(rawNameOf.apply(item))) {
                throw RecipeStepErrors.rawNameRequired(itemType, i);
            }
            if (isBlank(refOf.apply(item))) {
                throw RecipeStepErrors.refRequired(itemType, i);
            }
        }
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

        Map<String, BigDecimal> recipeIngredientQty = toQuantityMap(ingredients,
                RecipeStepResourceHelper::refKey, IngredientEmbeddedRequest::getQuantity, "ingredient", "recipe");
        Map<String, BigDecimal> recipeToolQty = toQuantityMap(tools,
                RecipeStepResourceHelper::refKey, ToolEmbeddedRequest::getQuantity, "tool", "recipe");
        Map<UUID, String> ingredientKeyById = keysByCatalogId(ingredients,
                IngredientEmbeddedRequest::getIngredientId, RecipeStepResourceHelper::refKey, "ingredient");
        Map<UUID, String> toolKeyById = keysByCatalogId(tools,
                ToolEmbeddedRequest::getToolId, RecipeStepResourceHelper::refKey, "tool");
        if (stepList.isEmpty()) {
            return;
        }

        Map<String, BigDecimal> usedIngredientQty = new HashMap<>();
        for (RecipeStepEmbeddedRequest step : stepList) {
            List<String> stepIngredientKeys = new ArrayList<>();
            for (IngredientEmbeddedRequest ingredient : nullToEmpty(step.getIngredients())) {
                String key = stepKey(ingredient.getRef(), ingredient.getIngredientId(), ingredientKeyById);
                String idField = stepIdField(ingredient.getRef(), "ingredientId");
                Object idValue = stepIdValue(ingredient.getRef(), ingredient.getIngredientId());
                requireInRecipe(recipeIngredientQty, key, idField, idValue, step.getStepNumber());
                stepIngredientKeys.add(key);
                usedIngredientQty.merge(key, qty(ingredient.getQuantity()), BigDecimal::add);
            }
            List<String> stepToolKeys = new ArrayList<>();
            for (ToolEmbeddedRequest tool : nullToEmpty(step.getTools())) {
                String key = stepKey(tool.getRef(), tool.getToolId(), toolKeyById);
                String idField = stepIdField(tool.getRef(), "toolId");
                Object idValue = stepIdValue(tool.getRef(), tool.getToolId());
                requireInRecipe(recipeToolQty, key, idField, idValue, step.getStepNumber());
                requireToolWithinRecipe(recipeToolQty, key, idField, idValue, tool.getQuantity(), step.getStepNumber());
                stepToolKeys.add(key);
            }
            validateUnique(stepIngredientKeys, "ingredient");
            validateUnique(stepToolKeys, "tool");
        }

        for (IngredientEmbeddedRequest ingredient : ingredients) {
            String key = refKey(ingredient);
            BigDecimal recipeQty = recipeIngredientQty.get(key);
            BigDecimal used = usedIngredientQty.getOrDefault(key, BigDecimal.ZERO);
            if (used.compareTo(recipeQty) != 0) {
                boolean matched = ingredient.getIngredientId() != null;
                throw RecipeStepErrors.ingredientTotalMismatch(matched ? "ingredientId" : "ref",
                        matched ? ingredient.getIngredientId() : key, used, recipeQty);
            }
        }
    }

    /**
     * Maps each non-null catalog id to its row's reference key; a catalog id may appear only once per list.
     */
    private static <T> Map<UUID, String> keysByCatalogId(List<T> items, Function<T, UUID> idOf,
                                                       Function<T, String> keyOf, String resource) {
        Map<UUID, String> result = new HashMap<>();
        for (T item : items) {
            UUID id = idOf.apply(item);
            if (id != null && result.put(id, keyOf.apply(item)) != null) {
                throw RecipeStepErrors.duplicateIds(resource, "recipe");
            }
        }
        return result;
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
            requireInRecipe(recipeIngredientQty, id, "recipeIngredientId", id, stepNumber);
            BigDecimal total = usedIngredientQty.getOrDefault(id, BigDecimal.ZERO).add(qty(ingredient.getQuantity()));
            BigDecimal recipeQty = recipeIngredientQty.get(id);
            if (total.compareTo(recipeQty) > 0) {
                throw RecipeStepErrors.ingredientTotalExceeds("recipeIngredientId", id, stepNumber, total, recipeQty);
            }
        }
        for (StepToolEmbeddedRequest tool : nullToEmpty(tools)) {
            UUID toolId = tool.getRecipeToolId();
            requireInRecipe(recipeToolQty, toolId, "recipeToolId", toolId, stepNumber);
            requireToolWithinRecipe(recipeToolQty, toolId, "recipeToolId", toolId, tool.getQuantity(), stepNumber);
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

    private static <T> T requirePresent(T value, String idField, Object id, Integer stepNumber) {
        if (value == null) {
            throw RecipeStepErrors.notInRecipe(idField, id, stepNumber);
        }
        return value;
    }

    private static <T> Map<String, BigDecimal> toQuantityMap(List<T> items, Function<T, String> keyOf,
                                                            Function<T, BigDecimal> qtyOf, String resource, String scope) {
        Map<String, BigDecimal> result = new HashMap<>();
        for (int i = 0; i < items.size(); i++) {
            T item = items.get(i);
            String key = keyOf.apply(item);
            if (key == null) {
                throw RecipeStepErrors.refRequired(resource, i);
            }
            if (result.put(key, qty(qtyOf.apply(item))) != null) {
                throw RecipeStepErrors.duplicateIds(resource, scope);
            }
        }
        return result;
    }

    private static <K> void requireInRecipe(Map<K, BigDecimal> recipeQty, K key, String idField, Object idValue,
                                            Integer stepNumber) {
        if (key == null || !recipeQty.containsKey(key)) {
            throw RecipeStepErrors.notInRecipe(idField, idValue, stepNumber);
        }
    }

    private static <K> void requireToolWithinRecipe(Map<K, BigDecimal> recipeToolQty, K key, String idField,
                                                    Object idValue, BigDecimal quantity, Integer stepNumber) {
        BigDecimal recipeQty = recipeToolQty.get(key);
        if (qty(quantity).compareTo(recipeQty) > 0) {
            throw RecipeStepErrors.toolQuantityExceeds(idField, idValue, stepNumber, qty(quantity), recipeQty);
        }
    }

    private static BigDecimal qty(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }

    private static <T> List<T> nullToEmpty(List<T> list) {
        return list == null ? List.of() : list;
    }

    private static void validateUnique(List<?> ids, String resource) {
        if (new HashSet<>(ids).size() != ids.size()) {
            throw RecipeStepErrors.duplicateIds(resource, "recipe step");
        }
    }
}
