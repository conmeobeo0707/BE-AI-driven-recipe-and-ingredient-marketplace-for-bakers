package com.manhnv.capstonebakerai.recommendation.engine;

import com.manhnv.capstonebakerai.recommendation.engine.RecommendationData.*;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.*;

// Scores recipes for one customer and picks the list to show.
//
// For each candidate recipe five features (0..1) are computed:
//   content       - recipe ingredients/category vs the customer's taste (content-based)
//   skill         - recipe difficulty vs the customer's skill level (knowledge base table)
//   hobby         - categories liked by people with the customer's hobbies (learned knowledge)
//   collaborative - "people who liked what you liked also liked this" (collaborative filtering)
//   quality       - rating and popularity
// score = sum of weight x feature; the weights are the active trained model's or the defaults.
// Bookmarked/rated recipes are skipped, and at most 3 recipes per category are shown.
@Component
public class Scorer {

    private static final int MAX_PER_CATEGORY = 3;
    private static final double MIN_REASON_CONTRIBUTION = 0.03;
    private static final Locale VI = Locale.forLanguageTag("vi-VN");

    // Everything needed to score recipes for one customer at one moment.
    public record Context(
            Snapshot snapshot,
            CommunityStats stats,
            CustomerData customer,
            TasteProfile taste,
            CommunityStats.Collaborative collaborative,
            Instant now
    ) {
    }

    public Context prepare(Snapshot snapshot, CommunityStats stats, CustomerData customer, Instant now) {
        TasteProfile taste = TasteProfile.build(customer, snapshot.recipesById(), now);
        return new Context(snapshot, stats, customer, taste, stats.collaborative(customer.id(), taste.recentLikes()), now);
    }

    // Published recipes that existed at that moment and that the customer has not bookmarked or rated.
    public List<RecipeItem> candidates(Context context) {
        return context.snapshot().recipes().stream()
                .filter(recipe -> !context.taste().excludedRecipes().contains(recipe.id()))
                .filter(recipe -> recipe.createdAt() == null || recipe.createdAt().isBefore(context.now()))
                .toList();
    }

    public Features features(RecipeItem recipe, Context context) {
        return new Features(
                content(recipe, context.taste()),
                skill(recipe, context),
                bestHobby(recipe, context).map(Map.Entry::getValue).orElse(0.0),
                context.collaborative().scores().getOrDefault(recipe.id(), 0.0),
                context.stats().quality(recipe.id())
        );
    }

    public List<ScoredRecipe> rank(Context context, Weights weights, int limit, boolean withReasons) {
        Weights used = context.taste().hasHistory() ? weights : weights.forNewCustomer();
        List<ScoredRecipe> scored = new ArrayList<>();
        for (RecipeItem recipe : candidates(context)) {
            Features features = features(recipe, context);
            scored.add(new ScoredRecipe(recipe.id(), used.score(features), features, List.of()));
        }
        scored.sort(Comparator.comparingDouble(ScoredRecipe::score).reversed().thenComparing(ScoredRecipe::recipeId));

        // At most 3 per category; if that leaves the list short, fill it with the best of the rest.
        List<ScoredRecipe> picked = new ArrayList<>();
        List<ScoredRecipe> skipped = new ArrayList<>();
        Map<UUID, Integer> perCategory = new HashMap<>();
        UUID noCategory = new UUID(0, 0);
        for (ScoredRecipe item : scored) {
            if (picked.size() >= limit) {
                break;
            }
            UUID category = Objects.requireNonNullElse(context.snapshot().recipesById().get(item.recipeId()).categoryId(), noCategory);
            if (perCategory.merge(category, 1, Integer::sum) <= MAX_PER_CATEGORY) {
                picked.add(item);
            } else {
                skipped.add(item);
            }
        }
        for (ScoredRecipe item : skipped) {
            if (picked.size() < limit) {
                picked.add(item);
            }
        }
        if (!withReasons) {
            return picked;
        }
        return picked.stream()
                .map(item -> new ScoredRecipe(item.recipeId(), Math.round(item.score() * 10_000) / 10_000.0, item.features(),
                        reasons(context.snapshot().recipesById().get(item.recipeId()), item.features(), used, context)))
                .toList();
    }

    // ---------- the five features ----------

    // 60% cosine similarity between the recipe's ingredients and the customer's ingredient taste,
    // 40% how much the customer likes the recipe's category.
    static double content(RecipeItem recipe, TasteProfile taste) {
        double ingredientPart = 0;
        if (taste.ingredientNorm() > 0 && !recipe.ingredients().isEmpty()) {
            double dot = 0;
            for (UUID ingredient : recipe.ingredients().keySet()) {
                dot += Math.max(0, taste.ingredients().getOrDefault(ingredient, 0.0));
            }
            ingredientPart = dot / (Math.sqrt(recipe.ingredients().size()) * taste.ingredientNorm());
        }
        double categoryPart = 0;
        if (recipe.categoryId() != null && taste.maxCategory() > 0) {
            categoryPart = Math.max(0, taste.categories().getOrDefault(recipe.categoryId(), 0.0)) / taste.maxCategory();
        }
        return Math.max(0, Math.min(1, 0.6 * ingredientPart + 0.4 * categoryPart));
    }

    // Declared skill level -> knowledge base table; otherwise the difficulty the customer usually likes.
    static double skill(RecipeItem recipe, Context context) {
        if (context.customer().skillLevel() != null) {
            return context.snapshot().fit(context.customer().skillLevel(), recipe.difficulty());
        }
        TasteProfile taste = context.taste();
        if (taste.maxDifficulty() > 0 && recipe.difficulty() != null) {
            return taste.difficulties().getOrDefault(recipe.difficulty(), 0.0) / taste.maxDifficulty();
        }
        return 0;
    }

    // The customer's hobby whose learned link to the recipe's category is strongest.
    static Optional<Map.Entry<UUID, Double>> bestHobby(RecipeItem recipe, Context context) {
        UUID bestHobby = null;
        double best = 0;
        for (UUID hobbyId : context.customer().hobbyIds()) {
            double score = context.stats().hobbyScore(context.customer().id(), hobbyId, recipe.categoryId());
            if (score > best) {
                best = score;
                bestHobby = hobbyId;
            }
        }
        return bestHobby == null ? Optional.empty() : Optional.of(Map.entry(bestHobby, best));
    }

    // ---------- reasons ----------

    // The 1-2 features that added the most to the score, as Vietnamese sentences.
    static List<String> reasons(RecipeItem recipe, Features features, Weights weights, Context context) {
        double[] w = weights.toArray();
        double[] f = features.toArray();
        Integer[] order = {0, 1, 2, 3, 4};
        Arrays.sort(order, (a, b) -> Double.compare(w[b] * f[b], w[a] * f[a]));
        List<String> reasons = new ArrayList<>();
        for (int index : order) {
            if (reasons.size() == 2 || w[index] * f[index] < MIN_REASON_CONTRIBUTION) {
                break;
            }
            String reason = reason(index, recipe, context);
            if (reason != null && !reasons.contains(reason)) {
                reasons.add(reason);
            }
        }
        if (reasons.isEmpty()) {
            reasons.add("Gợi ý nổi bật dành cho bạn");
        }
        return reasons;
    }

    private static String reason(int feature, RecipeItem recipe, Context context) {
        return switch (feature) {
            case 0 -> {
                List<String> names = recipe.ingredients().entrySet().stream()
                        .filter(e -> context.taste().ingredients().getOrDefault(e.getKey(), 0.0) > 0)
                        .sorted((a, b) -> Double.compare(context.taste().ingredients().get(b.getKey()),
                                context.taste().ingredients().get(a.getKey())))
                        .limit(2)
                        .map(e -> e.getValue().toLowerCase(VI))
                        .toList();
                if (!names.isEmpty()) {
                    yield "Có nguyên liệu bạn hay dùng: " + String.join(", ", names);
                }
                yield recipe.categoryName() == null ? null : "Thuộc loại bánh bạn hay xem: " + recipe.categoryName();
            }
            case 1 -> context.customer().skillLevel() == null ? "Độ khó giống các món bạn hay chọn"
                    : switch (context.customer().skillLevel()) {
                        case BEGINNER -> "Phù hợp trình độ người mới";
                        case INTERMEDIATE -> "Phù hợp trình độ trung cấp";
                        case ADVANCED -> "Phù hợp trình độ nâng cao";
                    };
            case 2 -> bestHobby(recipe, context)
                    .map(e -> context.snapshot().hobbyNames().get(e.getKey()))
                    .map(name -> "Hợp với sở thích " + name)
                    .orElse(null);
            case 3 -> {
                RecipeItem source = context.snapshot().recipesById().get(context.collaborative().source().get(recipe.id()));
                yield source != null && source.title() != null
                        ? "Người thích \"" + source.title() + "\" cũng thích món này"
                        : "Người có gu giống bạn cũng thích";
            }
            case 4 -> {
                CommunityStats stats = context.stats();
                if (stats.ratingCount(recipe.id()) > 0 && stats.ratingAverage(recipe.id()) >= 4) {
                    yield String.format(VI, "Được đánh giá cao (%.1f★, %d lượt)",
                            stats.ratingAverage(recipe.id()), stats.ratingCount(recipe.id()));
                }
                yield stats.popularity(recipe.id()) > 0 ? "Đang được nhiều người quan tâm" : null;
            }
            default -> null;
        };
    }
}
