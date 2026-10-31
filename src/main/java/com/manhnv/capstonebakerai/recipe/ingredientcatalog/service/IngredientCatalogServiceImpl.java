package com.manhnv.capstonebakerai.recipe.ingredientcatalog.service;

import com.manhnv.capstonebakerai.common.exception.AppException;
import com.manhnv.capstonebakerai.recipe.enums.RecipeStatus;
import com.manhnv.capstonebakerai.recipe.ingredientcatalog.dto.*;
import com.manhnv.capstonebakerai.recipe.ingredientcatalog.entity.IngredientAlias;
import com.manhnv.capstonebakerai.recipe.ingredientcatalog.entity.IngredientCatalog;
import com.manhnv.capstonebakerai.recipe.ingredientcatalog.exception.IngredientCatalogErrorCode;
import com.manhnv.capstonebakerai.recipe.ingredientcatalog.repository.IngredientAliasRepository;
import com.manhnv.capstonebakerai.recipe.ingredientcatalog.repository.IngredientCatalogRepository;
import com.manhnv.capstonebakerai.recipe.repository.IngredientRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Locale;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class IngredientCatalogServiceImpl implements IngredientCatalogService {

    private final IngredientCatalogRepository catalogRepository;
    private final IngredientAliasRepository aliasRepository;
    private final IngredientRepository ingredientRepository;
    private final IngredientNormalizer normalizer;

    @Override
    public List<IngredientCatalogResponse> getAll(Boolean active) {
        return catalogRepository.findAllByOrderByCanonicalNameAsc().stream()
                .filter(c -> active == null || c.isActive() == active)
                .map(this::response).toList();
    }

    @Override
    public IngredientCatalogResponse getById(UUID id) {
        return response(find(id));
    }

    @Override
    @Transactional
    public IngredientCatalogResponse create(SaveIngredientCatalogRequest request) {
        catalogRepository.lockNameRegistry();
        String name = validName(request.canonicalName());
        String normalized = normalizer.normalize(name);
        ensureAvailable(normalized, null);
        IngredientCatalog catalog = new IngredientCatalog();
        catalog.setCanonicalName(name);
        catalog.setNormalizedName(normalized);
        catalog.setActive(true);
        saveCatalog(catalog);
        ensureSelfAlias(catalog, name, normalized);
        return response(catalog);
    }

    @Override
    @Transactional
    public IngredientCatalogResponse update(UUID id, SaveIngredientCatalogRequest request) {
        catalogRepository.lockNameRegistry();
        IngredientCatalog catalog = findForUpdate(id);
        String name = validName(request.canonicalName());
        String normalized = normalizer.normalize(name);
        ensureAvailable(normalized, id);
        ensureSelfAlias(catalog, catalog.getCanonicalName(), catalog.getNormalizedName());
        catalog.setCanonicalName(name);
        catalog.setNormalizedName(normalized);
        saveCatalog(catalog);
        ensureSelfAlias(catalog, name, normalized);
        return response(catalog);
    }

    @Override
    @Transactional
    public IngredientCatalogResponse setActive(UUID id, boolean active) {
        catalogRepository.lockNameRegistry();
        IngredientCatalog catalog = findForUpdate(id);
        if (!active && ingredientRepository.existsByCatalogIngredient_IdAndRecipe_RecipeStatus(id, RecipeStatus.PUBLISHED)) {
            throw new AppException(IngredientCatalogErrorCode.CATALOG_USED_BY_PUBLISHED_RECIPE);
        }
        catalog.setActive(active);
        return response(saveCatalog(catalog));
    }

    @Override
    public List<IngredientAliasResponse> getAliases(UUID id) {
        find(id);
        return aliasRepository.findAllByIngredient_IdOrderByAliasAsc(id).stream()
                .map(this::aliasResponse).toList();
    }

    @Override
    @Transactional
    public IngredientAliasResponse addAlias(UUID id, SaveIngredientAliasRequest request) {
        catalogRepository.lockNameRegistry();
        IngredientCatalog catalog = find(id);
        String name = validName(request.alias());
        String normalized = normalizer.normalize(name);
        ensureAvailable(normalized, id);
        if (aliasRepository.findByNormalizedAlias(normalized).isPresent()) {
            throw new AppException(IngredientCatalogErrorCode.ALIAS_ALREADY_EXISTS);
        }
        IngredientAlias alias = new IngredientAlias();
        alias.setIngredient(catalog);
        alias.setAlias(name);
        alias.setNormalizedAlias(normalized);
        alias.setLanguageCode(request.languageCode() == null ? null : request.languageCode().toLowerCase(Locale.ROOT));
        return aliasResponse(saveAlias(alias));
    }

    @Override
    @Transactional
    public IngredientAliasResponse updateAlias(UUID id, UUID aliasId, SaveIngredientAliasRequest request) {
        catalogRepository.lockNameRegistry();
        IngredientCatalog catalog = find(id);
        IngredientAlias alias = aliasRepository.findByIdAndIngredient_Id(aliasId, id)
                .orElseThrow(() -> new AppException(IngredientCatalogErrorCode.ALIAS_NOT_FOUND));
        String name = validName(request.alias());
        String normalized = normalizer.normalize(name);
        if (alias.getNormalizedAlias().equals(catalog.getNormalizedName())
                && !normalized.equals(catalog.getNormalizedName())) {
            throw new AppException(IngredientCatalogErrorCode.CANONICAL_ALIAS_REQUIRED);
        }
        ensureAvailable(normalized, id);
        aliasRepository.findByNormalizedAlias(normalized).ifPresent(existing -> {
            if (!existing.getId().equals(aliasId)) {
                throw new AppException(IngredientCatalogErrorCode.ALIAS_ALREADY_EXISTS);
            }
        });
        alias.setAlias(name);
        alias.setNormalizedAlias(normalized);
        alias.setLanguageCode(request.languageCode() == null ? null : request.languageCode().toLowerCase(Locale.ROOT));
        return aliasResponse(saveAlias(alias));
    }

    @Override
    @Transactional
    public void deleteAlias(UUID id, UUID aliasId) {
        catalogRepository.lockNameRegistry();
        IngredientCatalog catalog = find(id);
        IngredientAlias alias = aliasRepository.findByIdAndIngredient_Id(aliasId, id)
                .orElseThrow(() -> new AppException(IngredientCatalogErrorCode.ALIAS_NOT_FOUND));
        if (alias.getNormalizedAlias().equals(catalog.getNormalizedName())) {
            throw new AppException(IngredientCatalogErrorCode.CANONICAL_ALIAS_REQUIRED);
        }
        aliasRepository.delete(alias);
        aliasRepository.flush();
    }

    private String validName(String name) {
        if (name == null || name.isBlank() || name.length() > 150 || normalizer.normalize(name).isBlank()) {
            throw new AppException(IngredientCatalogErrorCode.INVALID_INGREDIENT_NAME);
        }
        return name.trim();
    }

    private void ensureAvailable(String normalized, UUID owner) {
        catalogRepository.findByNormalizedName(normalized).ifPresent(c -> {
            if (!c.getId().equals(owner)) {
                throw new AppException(IngredientCatalogErrorCode.NAME_ALREADY_IN_USE);
            }
        });
        aliasRepository.findByNormalizedAlias(normalized).ifPresent(a -> {
            if (!a.getIngredient().getId().equals(owner)) {
                throw new AppException(IngredientCatalogErrorCode.NAME_ALREADY_IN_USE);
            }
        });
    }

    private void ensureSelfAlias(IngredientCatalog catalog, String name, String normalized) {
        ensureAvailable(normalized, catalog.getId());
        if (aliasRepository.findByNormalizedAlias(normalized).isPresent()) {
            return;
        }
        IngredientAlias alias = new IngredientAlias();
        alias.setIngredient(catalog);
        alias.setAlias(name);
        alias.setNormalizedAlias(normalized);
        saveAlias(alias);
    }

    private IngredientCatalog find(UUID id) {
        return catalogRepository.findById(id)
                .orElseThrow(() -> new AppException(IngredientCatalogErrorCode.CATALOG_NOT_FOUND));
    }

    private IngredientCatalog findForUpdate(UUID id) {
        return catalogRepository.findForUpdate(id)
                .orElseThrow(() -> new AppException(IngredientCatalogErrorCode.CATALOG_NOT_FOUND));
    }

    private IngredientCatalog saveCatalog(IngredientCatalog catalog) {
        try {
            return catalogRepository.saveAndFlush(catalog);
        } catch (DataIntegrityViolationException exception) {
            throw new AppException(IngredientCatalogErrorCode.NAME_ALREADY_IN_USE);
        }
    }

    private IngredientAlias saveAlias(IngredientAlias alias) {
        try {
            return aliasRepository.saveAndFlush(alias);
        } catch (DataIntegrityViolationException exception) {
            throw new AppException(IngredientCatalogErrorCode.NAME_ALREADY_IN_USE);
        }
    }

    private IngredientCatalogResponse response(IngredientCatalog c) {
        return new IngredientCatalogResponse(c.getId(), c.getCanonicalName(), c.getNormalizedName(),
                c.isActive(), c.getCreatedAt(), c.getUpdatedAt());
    }

    private IngredientAliasResponse aliasResponse(IngredientAlias a) {
        return new IngredientAliasResponse(a.getId(), a.getIngredient().getId(), a.getAlias(),
                a.getNormalizedAlias(), a.getLanguageCode(), a.getCreatedAt());
    }
}
