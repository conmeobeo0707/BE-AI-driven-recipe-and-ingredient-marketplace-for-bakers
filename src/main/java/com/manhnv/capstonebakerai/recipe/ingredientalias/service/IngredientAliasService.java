package com.manhnv.capstonebakerai.recipe.ingredientalias.service;

import com.manhnv.capstonebakerai.common.exception.AppException;
import com.manhnv.capstonebakerai.recipe.entity.Ingredient;
import com.manhnv.capstonebakerai.recipe.ingredientalias.dto.IngredientAliasResponse;
import com.manhnv.capstonebakerai.recipe.ingredientalias.dto.SaveIngredientAliasRequest;
import com.manhnv.capstonebakerai.recipe.ingredientalias.entity.IngredientAlias;
import com.manhnv.capstonebakerai.recipe.ingredientalias.exception.IngredientAliasErrorCode;
import com.manhnv.capstonebakerai.recipe.ingredientalias.repository.IngredientAliasRepository;
import com.manhnv.capstonebakerai.recipe.repository.IngredientRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

// Ingredient names and aliases share one name space: a normalized name belongs to at most one ingredient,
// whether it is that ingredient's own name or one of its aliases.
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class IngredientAliasService {

    private final IngredientAliasRepository aliasRepository;
    private final IngredientRepository ingredientRepository;
    private final IngredientNormalizer normalizer;

    public List<IngredientAliasResponse> getAliases(UUID ingredientId) {
        findIngredient(ingredientId);
        return aliasRepository.findAllByIngredient_IdOrderByAliasAsc(ingredientId).stream()
                .map(IngredientAliasService::toResponse).toList();
    }

    @Transactional
    public IngredientAliasResponse add(UUID ingredientId, SaveIngredientAliasRequest request) {
        aliasRepository.lockNameRegistry();
        Ingredient ingredient = findIngredient(ingredientId);
        String name = validName(request.alias());
        String normalized = normalizer.normalize(name);
        ensureAliasAvailable(normalized, ingredientId, null);
        return toResponse(save(newAlias(ingredient, name, normalized, request.languageCode())));
    }

    @Transactional
    public IngredientAliasResponse update(UUID ingredientId, UUID aliasId, SaveIngredientAliasRequest request) {
        aliasRepository.lockNameRegistry();
        findIngredient(ingredientId);
        IngredientAlias alias = aliasRepository.findByIdAndIngredient_Id(aliasId, ingredientId)
                .orElseThrow(() -> new AppException(IngredientAliasErrorCode.ALIAS_NOT_FOUND));
        String name = validName(request.alias());
        String normalized = normalizer.normalize(name);
        ensureAliasAvailable(normalized, ingredientId, aliasId);
        alias.setAlias(name);
        alias.setNormalizedAlias(normalized);
        alias.setLanguageCode(languageCode(request.languageCode()));
        return toResponse(save(alias));
    }

    @Transactional
    public void delete(UUID ingredientId, UUID aliasId) {
        aliasRepository.lockNameRegistry();
        IngredientAlias alias = aliasRepository.findByIdAndIngredient_Id(aliasId, ingredientId)
                .orElseThrow(() -> new AppException(IngredientAliasErrorCode.ALIAS_NOT_FOUND));
        aliasRepository.delete(alias);
        aliasRepository.flush();
    }

    // Used when an ingredient or a recipe is saved together with new names for an ingredient.
    // Names the ingredient already has are skipped; a name owned by another ingredient fails the whole save.
    @Transactional
    public List<IngredientAliasResponse> addAll(Ingredient ingredient, List<SaveIngredientAliasRequest> requests) {
        if (requests == null || requests.isEmpty()) {
            return List.of();
        }
        aliasRepository.lockNameRegistry();
        Map<String, SaveIngredientAliasRequest> byName = new LinkedHashMap<>();
        Set<String> repeated = new LinkedHashSet<>();
        for (SaveIngredientAliasRequest request : requests) {
            String name = validName(request.alias());
            if (byName.putIfAbsent(normalizer.normalize(name), request) != null) {
                repeated.add(name);
            }
        }
        if (!repeated.isEmpty()) {
            throw new AppException(IngredientAliasErrorCode.DUPLICATE_NAMES_IN_REQUEST,
                    "The same name appears more than once in the request: " + String.join(", ", repeated));
        }

        byName.remove(normalizer.normalize(ingredient.getName()));
        Set<String> conflicts = new LinkedHashSet<>();
        for (IngredientAlias existing : aliasRepository.findAllByNormalizedAliasIn(byName.keySet())) {
            if (existing.getIngredient().getId().equals(ingredient.getId())) {
                byName.remove(existing.getNormalizedAlias());
            } else {
                conflicts.add(byName.get(existing.getNormalizedAlias()).alias().trim());
            }
        }
        for (Ingredient other : ingredientRepository.findAllByOrderByNameAscIdAsc()) {
            SaveIngredientAliasRequest request = byName.get(normalizer.normalize(other.getName()));
            if (request != null && !other.getId().equals(ingredient.getId())) {
                conflicts.add(request.alias().trim());
            }
        }
        if (!conflicts.isEmpty()) {
            throw new AppException(IngredientAliasErrorCode.NAME_ALREADY_IN_USE,
                    "These names already belong to another ingredient: " + String.join(", ", conflicts));
        }

        List<IngredientAliasResponse> saved = new ArrayList<>();
        byName.forEach((normalized, request) -> saved.add(toResponse(
                save(newAlias(ingredient, request.alias().trim(), normalized, request.languageCode())))));
        return saved;
    }

    // An ingredient's own name may not be another ingredient's name or alias. Callers hold lockNameRegistry().
    public void ensureIngredientNameAvailable(String normalized, UUID ingredientId) {
        aliasRepository.findByNormalizedAlias(normalized).ifPresent(alias -> {
            if (!alias.getIngredient().getId().equals(ingredientId)) {
                throw new AppException(IngredientAliasErrorCode.NAME_ALREADY_IN_USE);
            }
        });
        if (ownerOfIngredientName(normalized).filter(owner -> !owner.equals(ingredientId)).isPresent()) {
            throw new AppException(IngredientAliasErrorCode.NAME_ALREADY_IN_USE);
        }
    }

    public String validName(String name) {
        if (name == null || name.isBlank() || name.trim().length() > 150 || normalizer.normalize(name).isBlank()) {
            throw new AppException(IngredientAliasErrorCode.INVALID_INGREDIENT_NAME);
        }
        return name.trim();
    }

    public void lockNameRegistry() {
        aliasRepository.lockNameRegistry();
    }

    private void ensureAliasAvailable(String normalized, UUID ingredientId, UUID aliasId) {
        aliasRepository.findByNormalizedAlias(normalized).ifPresent(alias -> {
            if (alias.getId().equals(aliasId)) {
                return;
            }
            throw new AppException(alias.getIngredient().getId().equals(ingredientId)
                    ? IngredientAliasErrorCode.ALIAS_ALREADY_EXISTS
                    : IngredientAliasErrorCode.NAME_ALREADY_IN_USE);
        });
        if (ownerOfIngredientName(normalized).filter(owner -> !owner.equals(ingredientId)).isPresent()) {
            throw new AppException(IngredientAliasErrorCode.NAME_ALREADY_IN_USE);
        }
    }

    private Optional<UUID> ownerOfIngredientName(String normalized) {
        return ingredientRepository.findAllByOrderByNameAscIdAsc().stream()
                .filter(ingredient -> normalized.equals(normalizer.normalize(ingredient.getName())))
                .map(Ingredient::getId)
                .findFirst();
    }

    private Ingredient findIngredient(UUID ingredientId) {
        return ingredientRepository.findById(ingredientId)
                .orElseThrow(() -> new AppException(IngredientAliasErrorCode.INGREDIENT_NOT_FOUND));
    }

    private IngredientAlias newAlias(Ingredient ingredient, String name, String normalized, String languageCode) {
        IngredientAlias alias = new IngredientAlias();
        alias.setIngredient(ingredient);
        alias.setAlias(name);
        alias.setNormalizedAlias(normalized);
        alias.setLanguageCode(languageCode(languageCode));
        return alias;
    }

    private static String languageCode(String value) {
        return value == null || value.isBlank() ? null : value.toLowerCase(Locale.ROOT);
    }

    private IngredientAlias save(IngredientAlias alias) {
        try {
            return aliasRepository.saveAndFlush(alias);
        } catch (DataIntegrityViolationException exception) {
            throw new AppException(IngredientAliasErrorCode.NAME_ALREADY_IN_USE);
        }
    }

    public static IngredientAliasResponse toResponse(IngredientAlias alias) {
        return new IngredientAliasResponse(alias.getId(), alias.getIngredient().getId(), alias.getAlias(),
                alias.getNormalizedAlias(), alias.getLanguageCode(), alias.getCreatedAt());
    }
}
