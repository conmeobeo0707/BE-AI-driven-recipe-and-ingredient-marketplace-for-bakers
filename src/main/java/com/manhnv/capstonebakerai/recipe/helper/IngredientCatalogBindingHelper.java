package com.manhnv.capstonebakerai.recipe.helper;

import com.manhnv.capstonebakerai.common.exception.AppException;
import com.manhnv.capstonebakerai.recipe.entity.Ingredient;
import com.manhnv.capstonebakerai.recipe.ingredientcatalog.exception.IngredientCatalogErrorCode;
import com.manhnv.capstonebakerai.recipe.ingredientcatalog.service.IngredientResolver;
import com.manhnv.capstonebakerai.recipe.ingredientcatalog.repository.IngredientCatalogRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
public class IngredientCatalogBindingHelper {

    private final IngredientResolver ingredientResolver;
    private final IngredientCatalogRepository catalogRepository;

    @Transactional
    public void bind(Ingredient ingredient, String rawName) {
        if (rawName == null || rawName.isBlank()) {
            throw new AppException(
                    IngredientCatalogErrorCode.INVALID_INGREDIENT_NAME
            );
        }

        String name = rawName.trim();

        ingredient.setName(name);
        ingredient.setCatalogIngredient(
                ingredientResolver.resolveExact(name)
                        .flatMap(catalog -> catalogRepository.findActiveForBinding(catalog.getId()))
                        .orElse(null)
        );
    }
}
