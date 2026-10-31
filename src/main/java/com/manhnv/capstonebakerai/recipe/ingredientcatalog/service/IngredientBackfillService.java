package com.manhnv.capstonebakerai.recipe.ingredientcatalog.service;

import com.manhnv.capstonebakerai.recipe.ingredientcatalog.dto.IngredientBackfillPreviewResponse;
import com.manhnv.capstonebakerai.recipe.ingredientcatalog.dto.IngredientBackfillRequest;

public interface IngredientBackfillService {
    IngredientBackfillPreviewResponse preview(int limit);
    int apply(IngredientBackfillRequest request);
}
