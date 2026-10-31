package com.manhnv.capstonebakerai.recipe.ingredientcatalog.service;

import com.manhnv.capstonebakerai.recipe.ingredientcatalog.dto.*;
import java.util.List;
import java.util.UUID;

public interface IngredientCatalogService {
    List<IngredientCatalogResponse> getAll(Boolean active);
    IngredientCatalogResponse getById(UUID id);
    IngredientCatalogResponse create(SaveIngredientCatalogRequest request);
    IngredientCatalogResponse update(UUID id, SaveIngredientCatalogRequest request);
    IngredientCatalogResponse setActive(UUID id, boolean active);
    List<IngredientAliasResponse> getAliases(UUID id);
    IngredientAliasResponse addAlias(UUID id, SaveIngredientAliasRequest request);
    IngredientAliasResponse updateAlias(UUID id, UUID aliasId, SaveIngredientAliasRequest request);
    void deleteAlias(UUID id, UUID aliasId);
}
