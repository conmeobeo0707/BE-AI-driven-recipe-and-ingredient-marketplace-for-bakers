package com.manhnv.capstonebakerai.recommendation.engine;

import com.manhnv.capstonebakerai.recipe.enums.DifficultyEnum;
import com.manhnv.capstonebakerai.recommendation.engine.RecommendationData.CustomerData;
import com.manhnv.capstonebakerai.recommendation.engine.RecommendationData.RecipeItem;
import com.manhnv.capstonebakerai.recommendation.engine.RecommendationData.Signal;
import com.manhnv.capstonebakerai.recommendation.engine.RecommendationData.Snapshot;

import java.time.Duration;
import java.time.Instant;
import java.util.*;

// "What does this customer like?" learned from their own behaviour.
//
// Every signal gets points: view +1 (at most 3 per recipe), bookmark +4, rating 5/4/3/2/1 -> +4/+2/0/-2/-4.
// Points halve every 30 days, and only the last 180 days (max 500 signals) count.
// A recipe's points are added to each of its catalog ingredients (times the ingredient's rarity),
// its category and its difficulty.
public record TasteProfile(
        Map<UUID, Double> ingredients,      // catalog ingredient -> points
        double ingredientNorm,              // length of the positive part, for cosine similarity
        Map<UUID, Double> categories,       // recipe category -> points
        double maxCategory,
        Map<DifficultyEnum, Double> difficulties,
        double maxDifficulty,
        Set<UUID> excludedRecipes,          // bookmarked or rated: never recommended again
        List<UUID> recentLikes,             // up to 20 liked recipes, newest first (for "people who liked X")
        boolean hasHistory
) {
    private static final Duration HISTORY = Duration.ofDays(180);
    private static final int MAX_SIGNALS = 500;
    private static final double HALF_LIFE_DAYS = 30;
    private static final double MAX_VIEW_POINTS = 3;
    private static final int MAX_RECENT_LIKES = 20;

    public static TasteProfile build(CustomerData customer, Snapshot snapshot, Instant now) {
        Map<UUID, RecipeItem> recipes = snapshot.recipesById();
        List<Signal> newestFirst = new ArrayList<>(customer.signals());
        newestFirst.sort(Comparator.comparing(Signal::at).reversed());

        // 1. Points per recipe.
        Map<UUID, Double> viewPoints = new HashMap<>();
        Map<UUID, Double> otherPoints = new HashMap<>();
        Instant horizon = now.minus(HISTORY);
        int used = 0;
        for (Signal signal : newestFirst) {
            if (used++ >= MAX_SIGNALS || signal.at().isBefore(horizon)) {
                break;
            }
            double decay = Math.pow(0.5, Math.max(0, Duration.between(signal.at(), now).toSeconds() / 86_400.0) / HALF_LIFE_DAYS);
            switch (signal.type()) {
                case VIEW -> viewPoints.merge(signal.recipeId(), decay, Double::sum);
                case BOOKMARK -> otherPoints.merge(signal.recipeId(), 4 * decay, Double::sum);
                case RATING -> otherPoints.merge(signal.recipeId(), ratingPoints(signal.rating()) * decay, Double::sum);
            }
        }
        Map<UUID, Double> recipePoints = new HashMap<>(otherPoints);
        viewPoints.forEach((recipe, points) -> recipePoints.merge(recipe, Math.min(points, MAX_VIEW_POINTS), Double::sum));

        // 2. Spread each recipe's points over its ingredients, category and difficulty.
        Map<UUID, Double> ingredients = new HashMap<>();
        Map<UUID, Double> categories = new HashMap<>();
        Map<DifficultyEnum, Double> difficulties = new EnumMap<>(DifficultyEnum.class);
        boolean hasHistory = false;
        for (Map.Entry<UUID, Double> entry : recipePoints.entrySet()) {
            RecipeItem recipe = recipes.get(entry.getKey());
            if (recipe == null) {
                continue;
            }
            hasHistory = true;
            double points = entry.getValue();
            // Rare ingredients say more about taste than the ones every recipe uses.
            recipe.ingredients().keySet().forEach(id -> ingredients.merge(id, points * snapshot.rarity(id), Double::sum));
            if (recipe.categoryId() != null) {
                categories.merge(recipe.categoryId(), points, Double::sum);
            }
            if (points > 0 && recipe.difficulty() != null) {
                difficulties.merge(recipe.difficulty(), points, Double::sum);
            }
        }

        // 3. Exclusions and likes use the whole history.
        Set<UUID> excluded = new HashSet<>();
        LinkedHashSet<UUID> likes = new LinkedHashSet<>();
        for (Signal signal : newestFirst) {
            if (signal.excludesRecipe()) {
                excluded.add(signal.recipeId());
            }
            if (signal.liked() && likes.size() < MAX_RECENT_LIKES) {
                likes.add(signal.recipeId());
            }
        }

        return new TasteProfile(
                ingredients,
                Math.sqrt(ingredients.values().stream().mapToDouble(v -> v > 0 ? v * v : 0).sum()),
                categories,
                Math.max(0, categories.values().stream().mapToDouble(Double::doubleValue).max().orElse(0)),
                difficulties,
                difficulties.values().stream().mapToDouble(Double::doubleValue).max().orElse(0),
                excluded,
                List.copyOf(likes),
                hasHistory
        );
    }

    static double ratingPoints(int rating) {
        return switch (rating) {
            case 5 -> 4;
            case 4 -> 2;
            case 2 -> -2;
            case 1 -> -4;
            default -> 0;
        };
    }
}
