package com.manhnv.capstonebakerai.recipe.ingredientcatalog.service;

import com.manhnv.capstonebakerai.common.exception.AppException;
import com.manhnv.capstonebakerai.recipe.dto.response.PageResponse;
import com.manhnv.capstonebakerai.recipe.ingredientcatalog.dto.IngredientCatalogCandidate;
import com.manhnv.capstonebakerai.recipe.ingredientcatalog.dto.UnlinkedIngredientGroup;
import com.manhnv.capstonebakerai.recipe.ingredientcatalog.exception.IngredientCatalogErrorCode;
import com.manhnv.capstonebakerai.recipe.repository.IngredientRepository;
import com.manhnv.capstonebakerai.recipe.repository.UnlinkedIngredientNameCount;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class IngredientLinkServiceImpl implements IngredientLinkService {

    private static final int MAX_PAGE_SIZE = 100;
    private static final int MAX_KEYWORD_LENGTH = 150;
    private static final int CANDIDATE_LIMIT = 3;

    private final IngredientRepository ingredientRepository;
    private final IngredientNormalizer normalizer;
    private final IngredientCatalogLookupService lookupService;

    // Unlinked rows grouped by normalized name, largest groups first, so staff handles each name once.
    @Override
    @Transactional(readOnly = true)
    public PageResponse<UnlinkedIngredientGroup> unlinkedGroups(String keyword, int page, int size) {
        String trimmed = keyword == null ? "" : keyword.trim();
        if (page < 0 || size < 1 || size > MAX_PAGE_SIZE || trimmed.length() > MAX_KEYWORD_LENGTH) {
            throw new AppException(IngredientCatalogErrorCode.INVALID_LINK_PAGE);
        }
        String normalizedKeyword = normalizer.normalize(trimmed);

        Map<String, List<UnlinkedIngredientNameCount>> byName = new LinkedHashMap<>();
        for (UnlinkedIngredientNameCount count : ingredientRepository.countUnlinkedByName()) {
            byName.computeIfAbsent(normalizer.normalize(count.getName()), key -> new ArrayList<>()).add(count);
        }

        List<Group> groups = byName.entrySet().stream()
                .filter(entry -> normalizedKeyword.isEmpty() || entry.getKey().contains(normalizedKeyword))
                .map(entry -> new Group(entry.getKey(), entry.getValue().stream()
                        .sorted(Comparator.comparingLong(UnlinkedIngredientNameCount::getRowCount).reversed()
                                .thenComparing(UnlinkedIngredientNameCount::getName))
                        .toList()))
                .sorted(Comparator.comparingLong(Group::rowCount).reversed().thenComparing(Group::name))
                .toList();

        int from = (int) Math.min((long) page * size, groups.size());
        List<Group> pageGroups = groups.subList(from, Math.min(from + size, groups.size()));
        Map<String, List<IngredientCatalogCandidate>> candidates =
                lookupService.searchAll(pageGroups.stream().map(Group::name).toList(), CANDIDATE_LIMIT);

        List<UnlinkedIngredientGroup> content = pageGroups.stream()
                .map(group -> new UnlinkedIngredientGroup(
                        group.name(),
                        group.normalizedName(),
                        group.rowCount(),
                        group.counts().stream().map(UnlinkedIngredientNameCount::getName).toList(),
                        candidates.getOrDefault(group.name(), List.of())
                ))
                .toList();
        return PageResponse.<UnlinkedIngredientGroup>builder()
                .content(content)
                .totalElements(groups.size())
                .totalPages((groups.size() + size - 1) / size)
                .currentPage(page)
                .build();
    }

    // name is the most common spelling in the group.
    private record Group(String normalizedName, List<UnlinkedIngredientNameCount> counts) {
        String name() {
            return counts.get(0).getName();
        }

        long rowCount() {
            return counts.stream().mapToLong(UnlinkedIngredientNameCount::getRowCount).sum();
        }
    }
}
