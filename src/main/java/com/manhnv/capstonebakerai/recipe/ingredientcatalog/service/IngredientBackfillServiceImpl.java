package com.manhnv.capstonebakerai.recipe.ingredientcatalog.service;

import com.manhnv.capstonebakerai.common.exception.AppException;
import com.manhnv.capstonebakerai.recipe.ingredientcatalog.dto.*;
import com.manhnv.capstonebakerai.recipe.ingredientcatalog.entity.IngredientCatalog;
import com.manhnv.capstonebakerai.recipe.ingredientcatalog.exception.IngredientCatalogErrorCode;
import com.manhnv.capstonebakerai.recipe.ingredientcatalog.repository.IngredientCatalogRepository;
import com.manhnv.capstonebakerai.recipe.repository.IngredientRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

@Service
@RequiredArgsConstructor
public class IngredientBackfillServiceImpl implements IngredientBackfillService {

    private final IngredientRepository ingredientRepository;
    private final IngredientCatalogRepository catalogRepository;
    private final IngredientResolver resolver;
    private final IngredientCatalogLookupService lookupService;

    @Override
    @Transactional(readOnly = true)
    public IngredientBackfillPreviewResponse preview(int limit) {
        if (limit < 1 || limit > 200) {
            throw new AppException(IngredientCatalogErrorCode.INVALID_BACKFILL_LIMIT);
        }
        var ingredients = ingredientRepository.findByCatalogIngredientIsNullOrderById(PageRequest.of(0, limit));
        Map<String, Optional<IngredientCatalog>> exact = new HashMap<>();
        for (var ingredient : ingredients) {
            exact.computeIfAbsent(ingredient.getName(), resolver::resolveExact);
        }
        var unmatchedNames = exact.entrySet().stream()
                .filter(entry -> entry.getValue().isEmpty()).map(Map.Entry::getKey).toList();
        var candidates = lookupService.searchAll(unmatchedNames, 3);
        var items = ingredients.stream().map(ingredient -> {
            var proposed = exact.get(ingredient.getName());
            return new IngredientBackfillPreviewResponse.Item(
                    ingredient.getId(), ingredient.getRecipe().getId(), ingredient.getName(),
                    proposed.map(IngredientCatalog::getId).orElse(null),
                    proposed.map(IngredientCatalog::getCanonicalName).orElse(null),
                    candidates.getOrDefault(ingredient.getName(), List.of())
            );
        }).toList();
        return new IngredientBackfillPreviewResponse(ingredientRepository.countByCatalogIngredientIsNull(), items);
    }

    @Override
    @Transactional
    public int apply(IngredientBackfillRequest request) {
        var mappings = request.mappings();
        if (mappings == null || mappings.isEmpty() || mappings.size() > 200
                || mappings.stream().anyMatch(m -> m == null || m.ingredientId() == null
                || m.catalogIngredientId() == null || m.expectedName() == null || m.expectedName().isBlank())
                || mappings.stream().map(IngredientBackfillRequest.Mapping::ingredientId).distinct().count() != mappings.size()) {
            throw new AppException(IngredientCatalogErrorCode.INVALID_BACKFILL);
        }
        catalogRepository.lockNameRegistry();
        for (var mapping : mappings.stream().sorted(Comparator.comparing(IngredientBackfillRequest.Mapping::ingredientId)).toList()) {
            var ingredient = ingredientRepository.findForBackfill(mapping.ingredientId())
                    .orElseThrow(() -> new AppException(IngredientCatalogErrorCode.BACKFILL_CONFLICT));
            if (ingredient.getCatalogIngredient() != null || !ingredient.getName().equals(mapping.expectedName())) {
                throw new AppException(IngredientCatalogErrorCode.BACKFILL_CONFLICT);
            }
            var catalog = catalogRepository.findActiveForBinding(mapping.catalogIngredientId())
                    .orElseThrow(() -> new AppException(IngredientCatalogErrorCode.CATALOG_INACTIVE));
            ingredient.setCatalogIngredient(catalog);
        }
        ingredientRepository.flush();
        return mappings.size();
    }
}
