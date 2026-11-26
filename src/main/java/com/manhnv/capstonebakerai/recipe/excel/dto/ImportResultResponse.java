package com.manhnv.capstonebakerai.recipe.excel.dto;

import com.manhnv.capstonebakerai.recipe.dto.response.RecipeResponse;

import java.util.List;

public record ImportResultResponse(boolean dryRun, int importedCount, List<RecipeResponse> recipes) {
}
