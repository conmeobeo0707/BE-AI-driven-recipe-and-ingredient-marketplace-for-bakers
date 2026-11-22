package com.manhnv.capstonebakerai.recipe.suggestion.service;

import com.manhnv.capstonebakerai.common.exception.AppException;
import com.manhnv.capstonebakerai.common.security.SecurityUtils;
import com.manhnv.capstonebakerai.customer.exception.CustomerErrorCode;
import com.manhnv.capstonebakerai.customer.repository.CustomerRepository;
import com.manhnv.capstonebakerai.recipe.entity.Ingredient;
import com.manhnv.capstonebakerai.recipe.entity.Recipe;
import com.manhnv.capstonebakerai.recipe.entity.RecipeIngredient;
import com.manhnv.capstonebakerai.recipe.enums.RecipeStatus;
import com.manhnv.capstonebakerai.recipe.repository.IngredientRepository;
import com.manhnv.capstonebakerai.recipe.repository.RecipeRepository;
import com.manhnv.capstonebakerai.recipe.suggestion.dto.RecipeSuggestionResponse;
import com.manhnv.capstonebakerai.recipe.suggestion.dto.RecipeSuggestionItem;
import com.manhnv.capstonebakerai.recipe.suggestion.exception.RecipeSuggestionErrorCode;
import com.manhnv.capstonebakerai.recipe.suggestion.repository.RecipeIngredientMatch;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class RecipeSuggestionServiceImpl implements RecipeSuggestionService {

    private static final int DEFAULT_LIMIT = 3;
    private static final int MAX_LIMIT = 5;

    private final CustomerRepository customerRepository;
    private final IngredientRepository ingredientRepository;
    private final RecipeRepository recipeRepository;

    @Override
    public RecipeSuggestionResponse suggest(List<UUID> ingredientIds, Integer requestedLimit) {
        if (ingredientIds == null || ingredientIds.isEmpty()
                || ingredientIds.size() > 20
                || ingredientIds.stream().anyMatch(Objects::isNull)) {
            throw new AppException(RecipeSuggestionErrorCode.INVALID_INGREDIENT_SELECTION);
        }
        int limit = validateLimit(requestedLimit);
        requireCustomer();

        Set<UUID> ids = new LinkedHashSet<>(ingredientIds);
        Map<UUID, Ingredient> selected = ingredientRepository.findAllSharedByIdIn(ids).stream()
                .collect(Collectors.toMap(Ingredient::getId, ingredient -> ingredient));
        if (selected.size() != ids.size()) {
            throw new AppException(RecipeSuggestionErrorCode.INVALID_INGREDIENT_SELECTION);
        }

        List<RecipeSuggestionResponse.SelectedIngredient> selectedIngredients = ids.stream()
                .map(id -> new RecipeSuggestionResponse.SelectedIngredient(id, selected.get(id).getName()))
                .toList();
        List<RecipeSuggestionItem> recipes = findRecipes(ids, limit);

        List<String> warnings = new ArrayList<>();
        warnings.add("Coverage compares ingredient types only; quantities are not checked.");
        if (recipes.isEmpty()) {
            warnings.add("No published recipe matched this selection.");
        }
        return new RecipeSuggestionResponse(selectedIngredients, recipes, warnings);
    }

    private List<RecipeSuggestionItem> findRecipes(Set<UUID> ids, int limit) {
        Set<UUID> recipeIds = recipeRepository.findIngredientMatches(RecipeStatus.PUBLISHED, ids).stream()
                .map(RecipeIngredientMatch::getRecipeId)
                .collect(Collectors.toCollection(LinkedHashSet::new));
        if (recipeIds.isEmpty()) {
            return List.of();
        }
        return recipeRepository.findSuggestionRecipesWithIngredients(recipeIds, RecipeStatus.PUBLISHED).stream()
                .filter(recipe -> recipe.getIngredients() != null && !recipe.getIngredients().isEmpty())
                .map(recipe -> toSuggestion(recipe, ids))
                .filter(item -> item.matchedIngredientCount() > 0)
                .sorted(Comparator.comparingDouble(RecipeSuggestionItem::coverage).reversed()
                        .thenComparing(RecipeSuggestionItem::matchedIngredientCount, Comparator.reverseOrder())
                        .thenComparing(RecipeSuggestionItem::title, Comparator.nullsLast(String.CASE_INSENSITIVE_ORDER))
                        .thenComparing(RecipeSuggestionItem::recipeId))
                .limit(limit).toList();
    }

    private RecipeSuggestionItem toSuggestion(Recipe recipe, Set<UUID> inputIngredientIds) {
        Map<UUID, Ingredient> ingredients = new LinkedHashMap<>();
        for (RecipeIngredient recipeIngredient : recipe.getIngredients()) {
            Ingredient ingredient = recipeIngredient.getIngredient();
            if (ingredient != null) {
                ingredients.putIfAbsent(ingredient.getId(), ingredient);
            }
        }
        List<String> matched = new ArrayList<>();
        List<String> missing = new ArrayList<>();
        for (Ingredient ingredient : ingredients.values()) {
            (inputIngredientIds.contains(ingredient.getId()) ? matched : missing).add(ingredient.getName());
        }
        matched.sort(String.CASE_INSENSITIVE_ORDER);
        missing.sort(String.CASE_INSENSITIVE_ORDER);
        int total = ingredients.size();
        //coverage = matched / total
        return new RecipeSuggestionItem(recipe.getId(), recipe.getTitle(), matched.size(), total,
                total == 0 ? 0 : (double) matched.size() / total, matched, missing);
    }

    private void requireCustomer() {
        customerRepository.findByUser_UserId(SecurityUtils.getCurrentUserId())
                .orElseThrow(() -> new AppException(CustomerErrorCode.CUSTOMER_PROFILE_NOT_FOUND));
    }

    private int validateLimit(Integer requestedLimit) {
        if (requestedLimit != null && (requestedLimit < 1 || requestedLimit > MAX_LIMIT)) {
            throw new AppException(RecipeSuggestionErrorCode.INVALID_SUGGESTION_LIMIT);
        }
        return requestedLimit == null ? DEFAULT_LIMIT : requestedLimit;
    }
}
