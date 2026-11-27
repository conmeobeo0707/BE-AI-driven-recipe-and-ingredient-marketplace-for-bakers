package com.manhnv.capstonebakerai.recipe.ingredientalias.service;

import com.manhnv.capstonebakerai.recipe.entity.Ingredient;
import com.manhnv.capstonebakerai.recipe.ingredientalias.entity.IngredientAlias;
import com.manhnv.capstonebakerai.recipe.ingredientalias.repository.IngredientAliasRepository;
import com.manhnv.capstonebakerai.recipe.repository.IngredientRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

// Turns any name (Vietnamese, English, another spelling) into an ingredient: aliases first, then ingredient names.
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class IngredientResolver {

    private final IngredientNormalizer normalizer;
    private final IngredientAliasRepository aliasRepository;
    private final IngredientRepository ingredientRepository;

    public Optional<Ingredient> resolveExact(String rawName) {
        String normalized = normalizer.normalize(rawName);
        if (normalized.isBlank()) {
            return Optional.empty();
        }
        Optional<Ingredient> byAlias = aliasRepository.findByNormalizedAlias(normalized).map(IngredientAlias::getIngredient);
        if (byAlias.isPresent()) {
            return byAlias;
        }
        return ingredientRepository.findAllByOrderByNameAscIdAsc().stream()
                .filter(ingredient -> normalized.equals(normalizer.normalize(ingredient.getName())))
                .findFirst();
    }
}
