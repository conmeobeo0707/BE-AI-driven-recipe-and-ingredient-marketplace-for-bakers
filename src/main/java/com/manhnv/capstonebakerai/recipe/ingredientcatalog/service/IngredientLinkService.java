package com.manhnv.capstonebakerai.recipe.ingredientcatalog.service;

import com.manhnv.capstonebakerai.recipe.dto.response.PageResponse;
import com.manhnv.capstonebakerai.recipe.ingredientcatalog.dto.UnlinkedIngredientGroup;

public interface IngredientLinkService {
    PageResponse<UnlinkedIngredientGroup> unlinkedGroups(String keyword, int page, int size);
}
