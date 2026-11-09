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
    private final IngredientAutoLinker autoLinker;

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
        autoLinker.linkUnlinked(catalog, List.of(normalized));
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
        autoLinker.linkUnlinked(catalog, List.of(normalized));
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
        saveCatalog(catalog);
        if (active) {
            autoLinker.linkUnlinked(catalog, aliasRepository.findAllByIngredient_IdOrderByAliasAsc(id).stream()
                    .map(IngredientAlias::getNormalizedAlias).toList());
        }
        return response(catalog);
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
        ensureAliasAvailable(normalized, id, null);
        IngredientAlias alias = saveAlias(newAlias(catalog, name, normalized, request.languageCode()));
        autoLinker.linkUnlinked(catalog, List.of(normalized));
        return aliasResponse(alias);
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
        ensureAliasAvailable(normalized, id, aliasId);
        alias.setAlias(name);
        alias.setNormalizedAlias(normalized);
        alias.setLanguageCode(languageCode(request.languageCode()));
        IngredientAlias saved = saveAlias(alias);
        autoLinker.linkUnlinked(catalog, List.of(normalized));
        return aliasResponse(saved);
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

    @Override
    @Transactional
    public LinkByNameResponse linkByName(LinkByNameRequest request) {
        catalogRepository.lockNameRegistry();
        IngredientCatalog catalog = catalogRepository.findActiveForBinding(request.catalogIngredientId())
                .orElseThrow(() -> new AppException(IngredientCatalogErrorCode.CATALOG_INACTIVE));
        String name = validName(request.name());
        String normalized = normalizer.normalize(name);
        boolean known = catalog.getNormalizedName().equals(normalized)
                || aliasRepository.findByNormalizedAlias(normalized)
                .filter(a -> a.getIngredient().getId().equals(catalog.getId())).isPresent();
        if (!known) {
            ensureAliasAvailable(normalized, catalog.getId(), null);
            saveAlias(newAlias(catalog, name, normalized, null));
        }
        int linked = autoLinker.linkUnlinked(catalog, List.of(normalized));
        return new LinkByNameResponse(catalog.getId(), catalog.getCanonicalName(), name, !known, linked);
    }

    private IngredientAlias newAlias(IngredientCatalog catalog, String name, String normalized, String languageCode) {
        IngredientAlias alias = new IngredientAlias();
        alias.setIngredient(catalog);
        alias.setAlias(name);
        alias.setNormalizedAlias(normalized);
        alias.setLanguageCode(languageCode(languageCode));
        return alias;
    }

    private String languageCode(String value) {
        return value == null ? null : value.toLowerCase(Locale.ROOT);
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

    // One lookup per table: another catalog's name/alias is NAME_ALREADY_IN_USE, another alias of this catalog is ALIAS_ALREADY_EXISTS.
    private void ensureAliasAvailable(String normalized, UUID catalogId, UUID aliasId) {
        catalogRepository.findByNormalizedName(normalized).ifPresent(c -> {
            if (!c.getId().equals(catalogId)) {
                throw new AppException(IngredientCatalogErrorCode.NAME_ALREADY_IN_USE);
            }
        });
        aliasRepository.findByNormalizedAlias(normalized).ifPresent(a -> {
            if (a.getId().equals(aliasId)) {
                return;
            }
            throw new AppException(a.getIngredient().getId().equals(catalogId)
                    ? IngredientCatalogErrorCode.ALIAS_ALREADY_EXISTS
                    : IngredientCatalogErrorCode.NAME_ALREADY_IN_USE);
        });
    }

    private void ensureSelfAlias(IngredientCatalog catalog, String name, String normalized) {
        ensureAvailable(normalized, catalog.getId());
        if (aliasRepository.findByNormalizedAlias(normalized).isPresent()) {
            return;
        }
        saveAlias(newAlias(catalog, name, normalized, null));
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
