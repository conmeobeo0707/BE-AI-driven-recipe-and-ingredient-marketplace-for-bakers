package com.manhnv.capstonebakerai.recipe.suggestion.service;

import com.manhnv.capstonebakerai.common.exception.AppException;
import com.manhnv.capstonebakerai.common.security.SecurityUtils;
import com.manhnv.capstonebakerai.customer.exception.CustomerErrorCode;
import com.manhnv.capstonebakerai.customer.repository.CustomerRepository;
import com.manhnv.capstonebakerai.recipe.entity.Ingredient;
import com.manhnv.capstonebakerai.recipe.entity.Recipe;
import com.manhnv.capstonebakerai.recipe.entity.RecipeIngredient;
import com.manhnv.capstonebakerai.recipe.enums.RecipeStatus;
import com.manhnv.capstonebakerai.recipe.ingredientcatalog.entity.IngredientCatalog;
import com.manhnv.capstonebakerai.recipe.ingredientcatalog.repository.IngredientCatalogRepository;
import com.manhnv.capstonebakerai.recipe.ingredientcatalog.service.IngredientNormalizer;
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
    private final IngredientCatalogRepository catalogRepository;
    private final RecipeRepository recipeRepository;
    private final IngredientNormalizer ingredientNormalizer;

    @Override
    public RecipeSuggestionResponse suggest(List<UUID> catalogIngredientIds, Integer requestedLimit) {
        if (catalogIngredientIds == null || catalogIngredientIds.isEmpty()
                || catalogIngredientIds.size() > 20
                || catalogIngredientIds.stream().anyMatch(Objects::isNull)) {
            throw new AppException(RecipeSuggestionErrorCode.INVALID_CATALOG_SELECTION);
        }
        int limit = validateLimit(requestedLimit);
        requireCustomer();

        Set<UUID> ids = new LinkedHashSet<>(catalogIngredientIds);
        Map<UUID, IngredientCatalog> selected = catalogRepository.findAllByIdInAndActiveTrue(ids).stream()
                .collect(Collectors.toMap(IngredientCatalog::getId, catalog -> catalog));
        if (selected.size() != ids.size()) {
            throw new AppException(RecipeSuggestionErrorCode.INVALID_CATALOG_SELECTION);
        }

        List<RecipeSuggestionResponse.SelectedIngredient> selectedIngredients = ids.stream()
                .map(id -> new RecipeSuggestionResponse.SelectedIngredient(id, selected.get(id).getCanonicalName()))
                .toList();
        List<RecipeSuggestionItem> recipes = findRecipes(ids, limit);

        List<String> warnings = new ArrayList<>();
        warnings.add("Coverage compares ingredient types only; quantities are not checked.");
        if (recipes.isEmpty()) {
            warnings.add("No published recipe matched this selection.");
        }
        return new RecipeSuggestionResponse(selectedIngredients, recipes, "INGREDIENT_TYPES_ONLY", warnings);
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

    // Unmapped or inactive-catalog ingredients stay in the recipe as missing (by raw name) instead of hiding it.
    private RecipeSuggestionItem toSuggestion(Recipe recipe, Set<UUID> inputCatalogIds) {
        Map<UUID, IngredientCatalog> mapped = new LinkedHashMap<>();
        Map<String, String> unmapped = new LinkedHashMap<>();
        for (RecipeIngredient recipeIngredient : recipe.getIngredients()) {
            Ingredient ingredient = recipeIngredient.getIngredient();
            if (ingredient == null) continue;
            IngredientCatalog catalog = ingredient.getCatalogIngredient();
            if (catalog != null && catalog.isActive()) {
                mapped.putIfAbsent(catalog.getId(), catalog);
            } else if (ingredient.getName() != null && !ingredient.getName().isBlank()) {
                unmapped.putIfAbsent(ingredientNormalizer.normalize(ingredient.getName()), ingredient.getName().trim());
            }
        }
        List<String> matched = new ArrayList<>();
        List<String> missing = new ArrayList<>(unmapped.values());
        for (IngredientCatalog catalog : mapped.values()) {
            (inputCatalogIds.contains(catalog.getId()) ? matched : missing).add(catalog.getCanonicalName());
        }
        matched.sort(String.CASE_INSENSITIVE_ORDER);
        missing.sort(String.CASE_INSENSITIVE_ORDER);
        int total = mapped.size() + unmapped.size();
        return new RecipeSuggestionItem(recipe.getId(), recipe.getTitle(), matched.size(), total,
                total == 0 ? 0 : (double) matched.size() / total, matched, missing, unmapped.size());
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
