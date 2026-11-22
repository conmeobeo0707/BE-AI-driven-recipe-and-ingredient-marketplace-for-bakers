package com.manhnv.capstonebakerai.recipe.service.impl;

import com.manhnv.capstonebakerai.recipe.dto.request.IngredientRequest;
import com.manhnv.capstonebakerai.recipe.dto.response.IngredientResponse;
import com.manhnv.capstonebakerai.recipe.dto.response.PageResponse;
import com.manhnv.capstonebakerai.recipe.entity.Ingredient;
import com.manhnv.capstonebakerai.recipe.ingredientalias.dto.IngredientAliasResponse;
import com.manhnv.capstonebakerai.recipe.ingredientalias.service.IngredientAliasService;
import com.manhnv.capstonebakerai.recipe.ingredientalias.service.IngredientNormalizer;
import com.manhnv.capstonebakerai.recipe.mapper.IngredientMapper;
import com.manhnv.capstonebakerai.recipe.repository.IngredientRepository;
import com.manhnv.capstonebakerai.recipe.service.IngredientService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class IngredientServiceImpl implements IngredientService {

    private final IngredientRepository repository;
    private final IngredientAliasService aliasService;
    private final IngredientNormalizer normalizer;

    @Override
    public PageResponse<IngredientResponse> getAll(String search, Pageable pageable) {
        Pageable validatedPageable = PageRequest.of(
                pageable.getPageNumber(),
                pageable.getPageSize(),
                pageable.getSort()
        );

        Page<Ingredient> page = (search != null && !search.isBlank())
                ? repository.findByNameContainingIgnoreCase(search.trim(), validatedPageable)
                : repository.findAll(validatedPageable);

        List<IngredientResponse> list = page.map(IngredientMapper::toDTO).toList();

        return PageResponse.<IngredientResponse>builder()
                .content(list)
                .totalElements(page.getTotalElements())
                .totalPages(page.getTotalPages())
                .currentPage(page.getNumber())
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public IngredientResponse getById(UUID id) {
        Ingredient entity = repository.findById(id)
                .orElseThrow(() -> new RuntimeException("Ingredient not found"));
        return withAliases(entity, aliasService.getAliases(id));
    }

    @Override
    @Transactional
    public IngredientResponse create(IngredientRequest request) {
        aliasService.lockNameRegistry();
        Ingredient entity = IngredientMapper.toEntity(request);
        entity.setName(aliasService.validName(request.getName()));
        aliasService.ensureIngredientNameAvailable(normalizer.normalize(entity.getName()), null);
        Ingredient saved = repository.saveAndFlush(entity);
        aliasService.addAll(saved, request.getAliases());
        return withAliases(saved, aliasService.getAliases(saved.getId()));
    }

    @Override
    @Transactional
    public IngredientResponse update(UUID id, IngredientRequest request) {
        aliasService.lockNameRegistry();
        Ingredient existing = repository.findById(id)
                .orElseThrow(() -> new RuntimeException("Ingredient not found"));

        String name = aliasService.validName(request.getName());
        aliasService.ensureIngredientNameAvailable(normalizer.normalize(name), id);
        existing.setName(name);

        return withAliases(repository.save(existing), aliasService.getAliases(id));
    }

    @Override
    public void delete(UUID id) {
        if (!repository.existsById(id)) {
            throw new RuntimeException("Ingredient not found");
        }
        repository.deleteById(id);
    }

    private static IngredientResponse withAliases(Ingredient ingredient, List<IngredientAliasResponse> aliases) {
        IngredientResponse response = IngredientMapper.toDTO(ingredient);
        response.setAliases(aliases);
        return response;
    }
}
