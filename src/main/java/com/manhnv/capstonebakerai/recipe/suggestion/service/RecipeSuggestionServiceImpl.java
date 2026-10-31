package com.manhnv.capstonebakerai.recipe.suggestion.service;

import com.manhnv.capstonebakerai.common.exception.AppException;
import com.manhnv.capstonebakerai.common.security.SecurityUtils;
import com.manhnv.capstonebakerai.customer.exception.CustomerErrorCode;
import com.manhnv.capstonebakerai.customer.repository.CustomerRepository;
import com.manhnv.capstonebakerai.recipe.entity.Ingredient;
import com.manhnv.capstonebakerai.recipe.entity.Recipe;
import com.manhnv.capstonebakerai.recipe.enums.RecipeStatus;
import com.manhnv.capstonebakerai.recipe.ingredientcatalog.entity.IngredientCatalog;
import com.manhnv.capstonebakerai.recipe.ingredientcatalog.repository.IngredientCatalogRepository;
import com.manhnv.capstonebakerai.recipe.ingredientcatalog.service.IngredientCatalogLookupService;
import com.manhnv.capstonebakerai.recipe.ingredientcatalog.service.IngredientResolver;
import com.manhnv.capstonebakerai.recipe.repository.RecipeRepository;
import com.manhnv.capstonebakerai.recipe.suggestion.dto.IngredientInputResult;
import com.manhnv.capstonebakerai.recipe.suggestion.dto.IngredientSuggestionResponse;
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
    private final IngredientResolver ingredientResolver;
    private final IngredientCatalogRepository catalogRepository;
    private final IngredientCatalogLookupService lookupService;
    private final RecipeRepository recipeRepository;

    @Override
    public IngredientSuggestionResponse suggest(
            List<String> ingredientNames, Integer requestedLimit, String inputMode, boolean allowPartial
    ) {
        validateNames(ingredientNames, false);
        int limit = validateLimit(requestedLimit);
        requireCustomer();

        List<IngredientInputResult> inputs = new ArrayList<>();
        Set<UUID> ids = new LinkedHashSet<>();
        for (String name : ingredientNames) {
            var resolved = ingredientResolver.resolveExact(name);
            if (resolved.isPresent()) {
                var catalog = resolved.get();
                ids.add(catalog.getId());
                inputs.add(new IngredientInputResult(name.trim(), true, catalog.getId(),
                        catalog.getCanonicalName(), List.of()));
            } else {
                inputs.add(new IngredientInputResult(name.trim(), false, null, null, List.of()));
            }
        }
        return assemble(inputMode, inputs, ids, limit, allowPartial);
    }

    @Override
    public IngredientSuggestionResponse suggestByIds(
            List<UUID> catalogIngredientIds, List<String> unmatchedIngredients,
            Integer requestedLimit, String inputMode, boolean allowPartial
    ) {
        var unmatched = unmatchedIngredients == null ? List.<String>of() : unmatchedIngredients;
        validateNames(unmatched, true);
        if (catalogIngredientIds == null || catalogIngredientIds.isEmpty()
                || catalogIngredientIds.size() + unmatched.size() > 20
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
        List<IngredientInputResult> inputs = new ArrayList<>();
        for (UUID id : ids) {
            IngredientCatalog catalog = selected.get(id);
            inputs.add(new IngredientInputResult(catalog.getCanonicalName(), true, id,
                    catalog.getCanonicalName(), List.of()));
        }
        for (String name : unmatched) {
            inputs.add(new IngredientInputResult(name.trim(), false, null, null, List.of()));
        }
        return assemble(inputMode, inputs, ids, limit, allowPartial);
    }

    private IngredientSuggestionResponse assemble(
            String inputMode, List<IngredientInputResult> inputs,
            Set<UUID> ids, int limit, boolean allowPartial
    ) {
        List<String> unmatched = inputs.stream().filter(item -> !item.matched())
                .map(IngredientInputResult::input).toList();
        var candidates = lookupService.searchAll(unmatched, 3);
        List<IngredientInputResult> enriched = inputs.stream().map(item -> new IngredientInputResult(
                item.input(), item.matched(), item.catalogIngredientId(), item.canonicalName(),
                item.matched() ? List.of() : candidates.getOrDefault(item.input(), List.of())
        )).toList();

        int matchedCount = (int) inputs.stream().filter(IngredientInputResult::matched).count();
        boolean requiresResolution = !unmatched.isEmpty() && (!allowPartial || ids.isEmpty());
        List<String> warnings = new ArrayList<>();
        warnings.add("Coverage compares ingredient types only; quantities are not checked.");
        if (!unmatched.isEmpty()) {
            warnings.add("Recognized " + matchedCount + "/" + inputs.size()
                    + " inputs. Unmatched ingredients are not used for recipe matching.");
            warnings.add(requiresResolution
                    ? "Select catalog ingredients, remove incorrect items, or explicitly allow partial matching."
                    : "Partial matching was accepted; recommendations use only the selected catalog ingredients.");
        }
        List<RecipeSuggestionItem> recipes = requiresResolution || ids.isEmpty()
                ? List.of() : findRecipes(ids, limit);
        if (ids.isEmpty()) {
            warnings.add("No catalog ingredient was recognized. Choose an ingredient from the catalog to continue.");
        } else if (!requiresResolution && recipes.isEmpty()) {
            warnings.add("No published recipe with complete active catalog mappings matched this selection.");
        }
        return new IngredientSuggestionResponse(inputMode, enriched, unmatched, recipes,
                inputs.size(), matchedCount, ids.size(), requiresResolution, "INGREDIENT_TYPES_ONLY", warnings);
    }

    private List<RecipeSuggestionItem> findRecipes(Set<UUID> ids, int limit) {
        Set<UUID> recipeIds = recipeRepository.findIngredientMatches(RecipeStatus.PUBLISHED, ids).stream()
                .map(RecipeIngredientMatch::getRecipeId)
                .collect(Collectors.toCollection(LinkedHashSet::new));
        if (recipeIds.isEmpty()) {
            return List.of();
        }
        return recipeRepository.findSuggestionRecipesWithIngredients(recipeIds, RecipeStatus.PUBLISHED).stream()
                .filter(this::hasCompleteCatalog)
                .map(recipe -> toSuggestion(recipe, ids))
                .filter(item -> item.matchedIngredientCount() > 0)
                .sorted(Comparator.comparingDouble(RecipeSuggestionItem::coverage).reversed()
                        .thenComparing(RecipeSuggestionItem::matchedIngredientCount, Comparator.reverseOrder())
                        .thenComparing(RecipeSuggestionItem::title, Comparator.nullsLast(String.CASE_INSENSITIVE_ORDER))
                        .thenComparing(RecipeSuggestionItem::recipeId))
                .limit(limit).toList();
    }

    private boolean hasCompleteCatalog(Recipe recipe) {
        List<Ingredient> ingredients = recipe.getIngredients();
        return ingredients != null && !ingredients.isEmpty()
                && ingredients.stream().allMatch(ingredient -> ingredient.getCatalogIngredient() != null
                && ingredient.getCatalogIngredient().isActive());
    }

    private RecipeSuggestionItem toSuggestion(Recipe recipe, Set<UUID> inputCatalogIds) {
        Map<UUID, IngredientCatalog> required = new LinkedHashMap<>();
        for (Ingredient ingredient : recipe.getIngredients()) {
            IngredientCatalog catalog = ingredient.getCatalogIngredient();
            required.putIfAbsent(catalog.getId(), catalog);
        }
        List<String> matched = new ArrayList<>();
        List<String> missing = new ArrayList<>();
        for (IngredientCatalog catalog : required.values()) {
            (inputCatalogIds.contains(catalog.getId()) ? matched : missing).add(catalog.getCanonicalName());
        }
        matched.sort(String.CASE_INSENSITIVE_ORDER);
        missing.sort(String.CASE_INSENSITIVE_ORDER);
        return new RecipeSuggestionItem(recipe.getId(), recipe.getTitle(), matched.size(), required.size(),
                (double) matched.size() / required.size(), matched, missing);
    }

    private void requireCustomer() {
        customerRepository.findByUser_UserId(SecurityUtils.getCurrentUserId())
                .orElseThrow(() -> new AppException(CustomerErrorCode.CUSTOMER_PROFILE_NOT_FOUND));
    }

    private void validateNames(List<String> names, boolean allowEmpty) {
        if (names == null || (!allowEmpty && names.isEmpty()) || names.size() > 20
                || names.stream().anyMatch(name -> name == null || name.isBlank() || name.length() > 120)) {
            throw new AppException(RecipeSuggestionErrorCode.INVALID_INGREDIENT_INPUT);
        }
    }

    private int validateLimit(Integer requestedLimit) {
        if (requestedLimit != null && (requestedLimit < 1 || requestedLimit > MAX_LIMIT)) {
            throw new AppException(RecipeSuggestionErrorCode.INVALID_SUGGESTION_LIMIT);
        }
        return requestedLimit == null ? DEFAULT_LIMIT : requestedLimit;
    }
}
