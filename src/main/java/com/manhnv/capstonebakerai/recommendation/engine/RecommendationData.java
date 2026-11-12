package com.manhnv.capstonebakerai.recommendation.engine;

import com.manhnv.capstonebakerai.customer.enums.SkillLevel;
import com.manhnv.capstonebakerai.recipe.enums.DifficultyEnum;

import java.time.Instant;
import java.util.*;

// The plain data types of the recommender, in one place. No logic beyond small helpers.
public final class RecommendationData {

    private RecommendationData() {
    }

    public enum SignalType {
        VIEW,
        BOOKMARK,
        RATING
    }

    // One thing a customer did with a recipe: viewed it, bookmarked it or rated it (rating 1-5, otherwise 0).
    public record Signal(UUID customerId, UUID recipeId, SignalType type, int rating, Instant at) {

        // A "like": a bookmark or a 4-5 star rating.
        public boolean liked() {
            return type == SignalType.BOOKMARK || (type == SignalType.RATING && rating >= 4);
        }

        // Bookmarked or rated recipes are never recommended again.
        public boolean excludesRecipe() {
            return type == SignalType.BOOKMARK || type == SignalType.RATING;
        }
    }

    // A published recipe as the recommender sees it. ingredients: catalog ingredient ID -> name.
    public record RecipeItem(
            UUID id,
            String title,
            UUID categoryId,
            String categoryName,
            DifficultyEnum difficulty,
            Instant createdAt,
            Map<UUID, String> ingredients
    ) {
    }

    // One customer: declared skill level and hobbies, plus their signals (oldest first).
    public record CustomerData(UUID id, SkillLevel skillLevel, Set<UUID> hobbyIds, List<Signal> signals) {

        // The same customer as they were at an earlier moment (only what happened strictly before it).
        public CustomerData before(Instant cutoff) {
            return new CustomerData(id, skillLevel, hobbyIds,
                    signals.stream().filter(signal -> signal.at().isBefore(cutoff)).toList());
        }

        // The same customer as if they had never touched this recipe. Used by training and evaluation:
        // a recipe is usually viewed just before it is liked, and that view must not help predict the like.
        public CustomerData without(UUID recipeId) {
            return new CustomerData(id, skillLevel, hobbyIds,
                    signals.stream().filter(signal -> !signal.recipeId().equals(recipeId)).toList());
        }
    }

    // Everything loaded from the database in one go: recipes, every customer's signals and the knowledge base.
    public record Snapshot(
            List<RecipeItem> recipes,
            Map<UUID, RecipeItem> recipesById,
            List<Signal> signals,
            Map<UUID, CustomerData> customers,
            Map<SkillLevel, Map<DifficultyEnum, Double>> skillFit,
            Map<UUID, String> hobbyNames,
            Map<UUID, Double> ingredientRarity,
            Instant loadedAt
    ) {
        public static Snapshot of(
                List<RecipeItem> recipes,
                List<Signal> signals,
                Map<UUID, SkillLevel> skillLevels,
                Map<UUID, Set<UUID>> hobbies,
                Map<SkillLevel, Map<DifficultyEnum, Double>> skillFit,
                Map<UUID, String> hobbyNames,
                Instant loadedAt
        ) {
            Map<UUID, RecipeItem> byId = new LinkedHashMap<>();
            recipes.forEach(recipe -> byId.put(recipe.id(), recipe));

            List<Signal> sorted = new ArrayList<>(signals);
            sorted.sort(Comparator.comparing(Signal::at));
            Map<UUID, List<Signal>> byCustomer = new HashMap<>();
            sorted.forEach(signal -> byCustomer.computeIfAbsent(signal.customerId(), id -> new ArrayList<>()).add(signal));

            Set<UUID> customerIds = new HashSet<>(skillLevels.keySet());
            customerIds.addAll(byCustomer.keySet());
            Map<UUID, CustomerData> customers = new HashMap<>();
            for (UUID id : customerIds) {
                customers.put(id, new CustomerData(id, skillLevels.get(id), hobbies.getOrDefault(id, Set.of()),
                        byCustomer.getOrDefault(id, List.of())));
            }
            // Rarity of an ingredient = ln(1 + recipes / recipes that use it): about 0.7 for one used everywhere
            // (flour, sugar), higher for a distinctive one (matcha), so common ingredients do not dominate taste.
            Map<UUID, Integer> usedIn = new HashMap<>();
            recipes.forEach(recipe -> recipe.ingredients().keySet().forEach(id -> usedIn.merge(id, 1, Integer::sum)));
            Map<UUID, Double> rarity = new HashMap<>();
            usedIn.forEach((id, count) -> rarity.put(id, Math.log(1 + (double) recipes.size() / count)));

            return new Snapshot(List.copyOf(recipes), byId, List.copyOf(sorted), customers, skillFit, hobbyNames,
                    rarity, loadedAt);
        }

        // 1 for an ingredient no published recipe uses any more (neutral).
        public double rarity(UUID ingredientId) {
            return ingredientRarity.getOrDefault(ingredientId, 1.0);
        }

        public CustomerData customer(UUID id) {
            return customers.getOrDefault(id, new CustomerData(id, null, Set.of(), List.of()));
        }

        // How well a difficulty suits a skill level (knowledge base table); 0.5 when unknown.
        public double fit(SkillLevel skillLevel, DifficultyEnum difficulty) {
            if (skillLevel == null || difficulty == null) {
                return 0.5;
            }
            return skillFit.getOrDefault(skillLevel, Map.of()).getOrDefault(difficulty, 0.5);
        }
    }

    // The five scores of one (customer, recipe) pair, each between 0 and 1.
    public record Features(double content, double skill, double hobby, double collaborative, double quality) {

        public double[] toArray() {
            return new double[]{content, skill, hobby, collaborative, quality};
        }
    }

    // How much each feature counts. Non-negative and normally summing to 1.
    public record Weights(double content, double skill, double hobby, double collaborative, double quality) {

        public static final int SIZE = 5;

        // Baseline for the evaluation: rank only by rating and popularity.
        public static Weights qualityOnly() {
            return new Weights(0, 0, 0, 0, 1);
        }

        public static Weights fromArray(double[] w) {
            return new Weights(w[0], w[1], w[2], w[3], w[4]);
        }

        public double[] toArray() {
            return new double[]{content, skill, hobby, collaborative, quality};
        }

        public double score(Features features) {
            double[] w = toArray();
            double[] f = features.toArray();
            double total = 0;
            for (int i = 0; i < SIZE; i++) {
                total += w[i] * f[i];
            }
            return total;
        }

        // After training: negative weights become 0 and the rest is scaled to sum to 1 (null if nothing is left).
        public Weights normalized() {
            double[] w = toArray();
            double total = 0;
            for (int i = 0; i < SIZE; i++) {
                w[i] = Double.isFinite(w[i]) ? Math.max(0, w[i]) : 0;
                total += w[i];
            }
            if (total <= 1e-9) {
                return null;
            }
            for (int i = 0; i < SIZE; i++) {
                w[i] = Math.round(w[i] / total * 10_000) / 10_000.0;
            }
            return fromArray(w);
        }

        // A customer with no history has no content or collaborative score,
        // so that share moves to skill, hobby and quality in proportion.
        public Weights forNewCustomer() {
            double moved = content + collaborative;
            double base = skill + hobby + quality;
            if (base <= 1e-9) {
                return qualityOnly();
            }
            return new Weights(0, skill + moved * skill / base, hobby + moved * hobby / base, 0,
                    quality + moved * quality / base);
        }
    }

    // One recommended recipe: its score, the five feature values and 1-2 reasons for the customer.
    public record ScoredRecipe(UUID recipeId, double score, Features features, List<String> reasons) {
    }

    // Offline evaluation of one weight set.
    // hitRate: share of test customers whose hidden liked recipe is in the top k;
    // mrr: average of 1/position of that recipe (0 when not in the top k);
    // coverage: share of published recipes that appear in at least one top-k list.
    public record Metrics(double hitRate, double mrr, double coverage) {
    }
}
