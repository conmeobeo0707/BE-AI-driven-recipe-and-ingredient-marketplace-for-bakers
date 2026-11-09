package com.manhnv.capstonebakerai.recipe.ingredientcatalog.service;

import com.manhnv.capstonebakerai.recipe.ingredientcatalog.entity.IngredientCatalog;
import com.manhnv.capstonebakerai.recipe.ingredientcatalog.repository.IngredientAliasRepository;
import com.manhnv.capstonebakerai.recipe.ingredientcatalog.repository.IngredientCatalogRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class IngredientResolver {

    private final IngredientNormalizer normalizer;
    private final IngredientCatalogRepository catalogRepository;
    private final IngredientAliasRepository aliasRepository;

    public Optional<IngredientCatalog> resolveExact(String rawName) {
        String normalized = normalizer.normalize(rawName);
        if (normalized.isBlank()) {
            return Optional.empty();
        }

        Optional<IngredientCatalog> canonical =
                catalogRepository.findByNormalizedNameAndActiveTrue(normalized);

        if (canonical.isPresent()) {
            return canonical;
        }

        return aliasRepository.findActiveIngredientByNormalizedAlias(normalized);
    }
}
