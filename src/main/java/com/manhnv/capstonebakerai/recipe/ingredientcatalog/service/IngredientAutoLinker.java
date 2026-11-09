package com.manhnv.capstonebakerai.recipe.ingredientcatalog.service;

import com.manhnv.capstonebakerai.recipe.ingredientcatalog.entity.IngredientCatalog;
import com.manhnv.capstonebakerai.recipe.repository.IngredientRepository;
import com.manhnv.capstonebakerai.recipe.repository.UnlinkedIngredientNameCount;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collection;
import java.util.List;
import java.util.Set;

// Links recipe ingredients that still have no catalog when their name normalizes exactly to one of the given names.
@Component
@RequiredArgsConstructor
public class IngredientAutoLinker {

    private final IngredientRepository ingredientRepository;
    private final IngredientNormalizer normalizer;

    @Transactional
    public int linkUnlinked(IngredientCatalog catalog, Collection<String> normalizedNames) {
        if (!catalog.isActive() || normalizedNames.isEmpty()) {
            return 0;
        }
        Set<String> targets = Set.copyOf(normalizedNames);
        List<String> names = ingredientRepository.countUnlinkedByName().stream()
                .map(UnlinkedIngredientNameCount::getName)
                .filter(name -> targets.contains(normalizer.normalize(name)))
                .toList();
        return names.isEmpty() ? 0 : ingredientRepository.linkUnlinkedByNames(catalog, names);
    }
}
