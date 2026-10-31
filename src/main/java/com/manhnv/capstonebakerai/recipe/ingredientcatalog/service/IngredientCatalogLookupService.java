package com.manhnv.capstonebakerai.recipe.ingredientcatalog.service;

import com.manhnv.capstonebakerai.common.exception.AppException;
import com.manhnv.capstonebakerai.recipe.ingredientcatalog.dto.IngredientCatalogCandidate;
import com.manhnv.capstonebakerai.recipe.ingredientcatalog.dto.IngredientCatalogCandidate.MatchType;
import com.manhnv.capstonebakerai.recipe.ingredientcatalog.exception.IngredientCatalogErrorCode;
import com.manhnv.capstonebakerai.recipe.ingredientcatalog.repository.IngredientAliasRepository;
import com.manhnv.capstonebakerai.recipe.ingredientcatalog.repository.IngredientCatalogRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class IngredientCatalogLookupService {

    private static final double MIN_SIMILARITY = 0.75;

    private static final Comparator<IngredientCatalogCandidate> ORDER = Comparator
            .comparing(IngredientCatalogCandidate::matchType)
            .thenComparing(IngredientCatalogCandidate::similarity, Comparator.reverseOrder())
            .thenComparing(IngredientCatalogCandidate::canonicalName, String.CASE_INSENSITIVE_ORDER)
            .thenComparing(IngredientCatalogCandidate::matchedName, String.CASE_INSENSITIVE_ORDER)
            .thenComparing(IngredientCatalogCandidate::catalogIngredientId);

    private final IngredientCatalogRepository catalogRepository;
    private final IngredientAliasRepository aliasRepository;
    private final IngredientNormalizer normalizer;

    public List<IngredientCatalogCandidate> search(String keyword, int limit) {
        if ((keyword != null && keyword.length() > 150) || limit < 1 || limit > 20) {
            throw new AppException(IngredientCatalogErrorCode.INVALID_SEARCH);
        }
        String query = normalizer.normalize(keyword);
        if (query.isEmpty() && keyword != null && !keyword.isBlank()) {
            throw new AppException(IngredientCatalogErrorCode.INVALID_SEARCH);
        }
        if (query.isEmpty()) {
            return catalogRepository.findAllByActiveTrueOrderByCanonicalNameAsc().stream()
                    .limit(limit)
                    .map(c -> new IngredientCatalogCandidate(
                            c.getId(), c.getCanonicalName(), c.getCanonicalName(), 0, MatchType.PARTIAL
                    ))
                    .toList();
        }
        return rank(query, terms(), limit);
    }

    public Map<String, List<IngredientCatalogCandidate>> searchAll(Collection<String> names, int limit) {
        if (limit < 1 || limit > 20) {
            throw new AppException(IngredientCatalogErrorCode.INVALID_SEARCH);
        }
        if (names.isEmpty()) {
            return Map.of();
        }
        List<Term> terms = terms();
        Map<String, List<IngredientCatalogCandidate>> result = new LinkedHashMap<>();
        for (String name : names) {
            String normalized = normalizer.normalize(name);
            result.put(name, normalized.isEmpty() ? List.of() : rank(normalized, terms, limit));
        }
        return result;
    }

    private List<Term> terms() {
        List<Term> terms = new ArrayList<>();
        for (var catalog : catalogRepository.findAllByActiveTrueOrderByCanonicalNameAsc()) {
            terms.add(new Term(catalog.getId(), catalog.getCanonicalName(),
                    catalog.getCanonicalName(), catalog.getNormalizedName()));
        }
        for (var alias : aliasRepository.findAllByIngredient_ActiveTrue()) {
            terms.add(new Term(alias.getIngredient().getId(), alias.getIngredient().getCanonicalName(),
                    alias.getAlias(), alias.getNormalizedAlias()));
        }
        return terms;
    }

    private List<IngredientCatalogCandidate> rank(String query, List<Term> terms, int limit) {
        Map<UUID, IngredientCatalogCandidate> best = new HashMap<>();
        for (Term term : terms) {
            MatchType type;
            double score;
            if (term.normalized().equals(query)) {
                type = MatchType.EXACT;
                score = 1;
            } else if (query.length() >= 2 && (term.normalized().contains(query)
                    || (term.normalized().length() >= 4
                    && (" " + query + " ").contains(" " + term.normalized() + " ")))) {
                type = MatchType.PARTIAL;
                score = (double) Math.min(query.length(), term.normalized().length())
                        / Math.max(query.length(), term.normalized().length());
            } else {
                if (query.length() < 3) {
                    continue;
                }
                int maxLength = Math.max(query.length(), term.normalized().length());
                if (Math.abs(query.length() - term.normalized().length()) > maxLength * (1 - MIN_SIMILARITY)) {
                    continue;
                }
                score = similarity(query, term.normalized());
                if (score < MIN_SIMILARITY) {
                    continue;
                }
                type = MatchType.FUZZY;
            }
            var candidate = new IngredientCatalogCandidate(term.id(), term.canonicalName(), term.name(),
                    Math.round(score * 1000) / 1000.0, type);
            best.merge(term.id(), candidate, (first, second) -> ORDER.compare(first, second) <= 0 ? first : second);
        }
        return best.values().stream().sorted(ORDER).limit(limit).toList();
    }

    private double similarity(String first, String second) {
        int[] previous = new int[second.length() + 1];
        int[] current = new int[second.length() + 1];
        for (int j = 0; j <= second.length(); j++) {
            previous[j] = j;
        }
        for (int i = 1; i <= first.length(); i++) {
            current[0] = i;
            for (int j = 1; j <= second.length(); j++) {
                int cost = first.charAt(i - 1) == second.charAt(j - 1) ? 0 : 1;
                current[j] = Math.min(Math.min(current[j - 1] + 1, previous[j] + 1), previous[j - 1] + cost);
            }
            int[] swap = previous;
            previous = current;
            current = swap;
        }
        return 1 - (double) previous[second.length()] / Math.max(first.length(), second.length());
    }

    private record Term(UUID id, String canonicalName, String name, String normalized) {
    }
}
