package com.manhnv.capstonebakerai.recipe.ingredientalias.service;

import com.manhnv.capstonebakerai.common.exception.AppException;
import com.manhnv.capstonebakerai.recipe.ingredientalias.dto.IngredientCandidate;
import com.manhnv.capstonebakerai.recipe.ingredientalias.dto.IngredientCandidate.MatchType;
import com.manhnv.capstonebakerai.recipe.ingredientalias.exception.IngredientAliasErrorCode;
import com.manhnv.capstonebakerai.recipe.ingredientalias.repository.IngredientAliasRepository;
import com.manhnv.capstonebakerai.recipe.repository.IngredientRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class IngredientLookupService {

    private static final double MIN_SIMILARITY = 0.75;

    private static final Comparator<IngredientCandidate> ORDER = Comparator
            .comparing(IngredientCandidate::matchType)
            .thenComparing(IngredientCandidate::similarity, Comparator.reverseOrder())
            .thenComparing(IngredientCandidate::ingredientName, String.CASE_INSENSITIVE_ORDER)
            .thenComparing(IngredientCandidate::matchedName, String.CASE_INSENSITIVE_ORDER)
            .thenComparing(IngredientCandidate::ingredientId);

    private final IngredientRepository ingredientRepository;
    private final IngredientAliasRepository aliasRepository;
    private final IngredientNormalizer normalizer;

    public List<IngredientCandidate> search(String keyword, int limit) {
        if ((keyword != null && keyword.length() > 150) || limit < 1 || limit > 20) {
            throw new AppException(IngredientAliasErrorCode.INVALID_SEARCH);
        }
        String query = normalizer.normalize(keyword);
        if (query.isEmpty() && keyword != null && !keyword.isBlank()) {
            throw new AppException(IngredientAliasErrorCode.INVALID_SEARCH);
        }
        if (query.isEmpty()) {
            return ingredientRepository.findAllShared().stream()
                    .limit(limit)
                    .map(i -> new IngredientCandidate(i.getId(), i.getName(), i.getName(), 0, null))
                    .toList();
        }
        return rank(query, terms(), limit);
    }

    public Map<String, List<IngredientCandidate>> searchAll(Collection<String> names, int limit) {
        if (limit < 1 || limit > 20) {
            throw new AppException(IngredientAliasErrorCode.INVALID_SEARCH);
        }
        if (names.isEmpty()) {
            return Map.of();
        }
        List<Term> terms = terms();
        Map<String, List<IngredientCandidate>> result = new LinkedHashMap<>();
        for (String name : names) {
            String normalized = normalizer.normalize(name);
            result.put(name, normalized.isEmpty() ? List.of() : rank(normalized, terms, limit));
        }
        return result;
    }

    private List<Term> terms() {
        List<Term> terms = new ArrayList<>();
        for (var ingredient : ingredientRepository.findAllShared()) {
            terms.add(new Term(ingredient.getId(), ingredient.getName(),
                    ingredient.getName(), normalizer.normalize(ingredient.getName())));
        }
        for (var alias : aliasRepository.findAll()) {
            terms.add(new Term(alias.getIngredient().getId(), alias.getIngredient().getName(),
                    alias.getAlias(), alias.getNormalizedAlias()));
        }
        return terms;
    }

    private List<IngredientCandidate> rank(String query, List<Term> terms, int limit) {
        Map<UUID, IngredientCandidate> best = new HashMap<>();
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
                // score = length of the shorter string / length of the longer string
                score = (double) Math.min(query.length(), term.normalized().length())
                        / Math.max(query.length(), term.normalized().length());
            } else {
                if (query.length() < 3) {
                    continue;
                }
                int maxLength = Math.max(query.length(), term.normalized().length());
                // if the length difference is too large, skip fuzzy matching
                if (Math.abs(query.length() - term.normalized().length()) > maxLength * (1 - MIN_SIMILARITY)) {
                    continue;
                }
                score = similarity(query, term.normalized());
                if (score < MIN_SIMILARITY) {
                    continue;
                }
                type = MatchType.FUZZY;
            }
            var candidate = new IngredientCandidate(term.id(), term.ingredientName(), term.name(),
                    Math.round(score * 1000) / 1000.0, type);
            best.merge(term.id(), candidate, (first, second) -> ORDER.compare(first, second) <= 0 ? first : second);
        }
        return best.values().stream().sorted(ORDER).limit(limit).toList();
    }

    //levenshtein distance similarity
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

    private record Term(UUID id, String ingredientName, String name, String normalized) {
    }
}
