package com.manhnv.capstonebakerai.recommendation.engine;

import com.manhnv.capstonebakerai.recommendation.engine.RecommendationData.RecipeItem;
import com.manhnv.capstonebakerai.recommendation.engine.RecommendationData.Signal;
import com.manhnv.capstonebakerai.recommendation.engine.RecommendationData.SignalType;
import com.manhnv.capstonebakerai.recommendation.engine.RecommendationData.Snapshot;

import java.time.Duration;
import java.time.Instant;
import java.util.*;

// "What does everybody like?" Counts over all customers, using only signals strictly before a cutoff
// (so the offline evaluation never peeks at the future). Three things are computed:
// 1. quality: Bayesian-adjusted rating + 30-day popularity (per customer and recipe: viewed +1 once, bookmark +3, rating +3);
// 2. collaborative: "people who liked X also liked Y";
// 3. hobby knowledge: which recipe categories the customers holding a hobby like more than average.
public final class CommunityStats {

    private static final double PRIOR_RATINGS = 5;
    private static final double DEFAULT_MEAN_RATING = 3.5;
    private static final Duration POPULARITY_WINDOW = Duration.ofDays(30);
    private static final int MIN_HOBBY_LIKES = 5;      // below this a hobby is not learned yet
    private static final double HOBBY_SMOOTHING = 5;   // small hobbies are pulled towards the average
    private static final double FULL_LIFT = 3;         // liked 3x more than average -> hobby score 1

    private final Map<UUID, double[]> ratings = new HashMap<>();       // recipe -> {sum, count}
    private final Map<UUID, Double> popularity = new HashMap<>();
    private final Map<UUID, Set<UUID>> likersByRecipe = new HashMap<>();
    private final Map<UUID, Set<UUID>> likesByCustomer = new HashMap<>();
    private final Map<UUID, Integer> categoryLikes = new HashMap<>();
    private final Map<UUID, Map<UUID, Integer>> hobbyCategoryLikes = new HashMap<>();
    private final Map<UUID, Integer> hobbyLikes = new HashMap<>();
    private final Map<UUID, Map<UUID, Integer>> customerCategoryLikes = new HashMap<>();
    private final Map<UUID, Set<UUID>> customerHobbies = new HashMap<>();
    private final double meanRating;
    private final double maxPopularity;
    private int totalLikes;

    private CommunityStats(Snapshot snapshot, Instant cutoff) {
        snapshot.customers().forEach((id, customer) -> customerHobbies.put(id, customer.hobbyIds()));
        Instant windowStart = cutoff.minus(POPULARITY_WINDOW);
        double ratingSum = 0;
        int ratingCount = 0;
        Set<String> counted = new HashSet<>();
        Set<String> viewed = new HashSet<>();
        for (Signal signal : snapshot.signals()) {
            if (!signal.at().isBefore(cutoff)) {
                continue;
            }
            if (signal.type() == SignalType.RATING && signal.rating() > 0) {
                double[] entry = ratings.computeIfAbsent(signal.recipeId(), id -> new double[2]);
                entry[0] += signal.rating();
                entry[1]++;
                ratingSum += signal.rating();
                ratingCount++;
            }
            // Popularity counts people, not clicks: a customer's views of a recipe add 1 once,
            // so reopening a recipe again and again cannot push it up for everybody.
            if (!signal.at().isBefore(windowStart)) {
                if (signal.type() != SignalType.VIEW) {
                    popularity.merge(signal.recipeId(), 3.0, Double::sum);
                } else if (viewed.add(signal.customerId() + ":" + signal.recipeId())) {
                    popularity.merge(signal.recipeId(), 1.0, Double::sum);
                }
            }
            if (signal.liked()) {
                likersByRecipe.computeIfAbsent(signal.recipeId(), id -> new HashSet<>()).add(signal.customerId());
                likesByCustomer.computeIfAbsent(signal.customerId(), id -> new HashSet<>()).add(signal.recipeId());
                RecipeItem recipe = snapshot.recipesById().get(signal.recipeId());
                // A bookmark and a 5-star rating of the same recipe count as one like.
                if (recipe != null && recipe.categoryId() != null
                        && counted.add(signal.customerId() + ":" + signal.recipeId())) {
                    countCategoryLike(signal.customerId(), recipe.categoryId());
                }
            }
        }
        this.meanRating = ratingCount == 0 ? DEFAULT_MEAN_RATING : ratingSum / ratingCount;
        this.maxPopularity = popularity.values().stream().mapToDouble(Double::doubleValue).max().orElse(0);
    }

    public static CommunityStats compute(Snapshot snapshot, Instant cutoff) {
        return new CommunityStats(snapshot, cutoff);
    }

    private void countCategoryLike(UUID customerId, UUID categoryId) {
        totalLikes++;
        categoryLikes.merge(categoryId, 1, Integer::sum);
        customerCategoryLikes.computeIfAbsent(customerId, id -> new HashMap<>()).merge(categoryId, 1, Integer::sum);
        for (UUID hobbyId : customerHobbies.getOrDefault(customerId, Set.of())) {
            hobbyLikes.merge(hobbyId, 1, Integer::sum);
            hobbyCategoryLikes.computeIfAbsent(hobbyId, id -> new HashMap<>()).merge(categoryId, 1, Integer::sum);
        }
    }

    // ---------- 1. Quality ----------

    public int ratingCount(UUID recipeId) {
        double[] entry = ratings.get(recipeId);
        return entry == null ? 0 : (int) entry[1];
    }

    public double ratingAverage(UUID recipeId) {
        double[] entry = ratings.get(recipeId);
        return entry == null ? 0 : entry[0] / entry[1];
    }

    public double popularity(UUID recipeId) {
        return maxPopularity <= 0 ? 0
                : Math.log1p(popularity.getOrDefault(recipeId, 0.0)) / Math.log1p(maxPopularity);
    }

    // 70% rating (5 "virtual" average votes are added, so one 5-star vote does not beat fifty 4.5-star votes)
    // + 30% popularity over the last 30 days. Between 0 and 1.
    public double quality(UUID recipeId) {
        double[] entry = ratings.get(recipeId);
        double sum = entry == null ? 0 : entry[0];
        double count = entry == null ? 0 : entry[1];
        double adjusted = (sum + PRIOR_RATINGS * meanRating) / (count + PRIOR_RATINGS);
        double rating = Math.max(0, Math.min(1, (adjusted - 1) / 4));
        return 0.7 * rating + 0.3 * popularity(recipeId);
    }

    // ---------- 2. Collaborative filtering ----------

    // Item-based: for every recipe the customer liked, look at who else liked it and what else they liked.
    // Similarity of two recipes = shared likers / sqrt(likers of A x likers of B). The customer is left out.
    public Collaborative collaborative(UUID customerId, Collection<UUID> likedRecipes) {
        Map<UUID, Double> raw = new HashMap<>();
        Map<UUID, UUID> source = new HashMap<>();
        Map<UUID, Double> best = new HashMap<>();
        for (UUID liked : likedRecipes) {
            Set<UUID> likers = without(likersByRecipe.get(liked), customerId);
            if (likers.isEmpty()) {
                continue;
            }
            Map<UUID, Integer> shared = new HashMap<>();
            for (UUID other : likers) {
                for (UUID recipe : likesByCustomer.getOrDefault(other, Set.of())) {
                    if (!recipe.equals(liked)) {
                        shared.merge(recipe, 1, Integer::sum);
                    }
                }
            }
            for (Map.Entry<UUID, Integer> entry : shared.entrySet()) {
                int otherLikers = without(likersByRecipe.get(entry.getKey()), customerId).size();
                if (otherLikers == 0) {
                    continue;
                }
                double similarity = entry.getValue() / Math.sqrt((double) likers.size() * otherLikers);
                raw.merge(entry.getKey(), similarity, Double::sum);
                if (similarity > best.getOrDefault(entry.getKey(), 0.0)) {
                    best.put(entry.getKey(), similarity);
                    source.put(entry.getKey(), liked);
                }
            }
        }
        // Scaled to 0..1 by the customer's best recipe, but never scaled UP: weak evidence stays a low score.
        double scale = Math.max(1, raw.values().stream().mapToDouble(Double::doubleValue).max().orElse(0));
        Map<UUID, Double> scores = new HashMap<>();
        raw.forEach((recipe, value) -> scores.put(recipe, value / scale));
        return new Collaborative(scores, source);
    }

    // scores: recipe -> 0..1; source: the liked recipe that contributed most (used in the reason text).
    public record Collaborative(Map<UUID, Double> scores, Map<UUID, UUID> source) {
    }

    private static Set<UUID> without(Set<UUID> customers, UUID excluded) {
        if (customers == null || !customers.contains(excluded)) {
            return customers == null ? Set.of() : customers;
        }
        Set<UUID> copy = new HashSet<>(customers);
        copy.remove(excluded);
        return copy;
    }

    // ---------- 3. Learned hobby knowledge ----------

    // lift = (share of the hobby's likes in this category, smoothed) / (share of all likes in this category).
    // score = 0 at lift <= 1, rising to 1 at lift >= 3. customerId (may be null) is left out of the counts,
    // so the score means "what people with your hobby like", not a copy of your own history.
    public HobbyCategory hobbyCategory(UUID customerId, UUID hobbyId, UUID categoryId) {
        if (categoryId == null || totalLikes == 0) {
            return null;
        }
        int inCategory = hobbyCategoryLikes.getOrDefault(hobbyId, Map.of()).getOrDefault(categoryId, 0);
        int likes = hobbyLikes.getOrDefault(hobbyId, 0);
        if (customerId != null && customerHobbies.getOrDefault(customerId, Set.of()).contains(hobbyId)) {
            Map<UUID, Integer> own = customerCategoryLikes.getOrDefault(customerId, Map.of());
            inCategory -= own.getOrDefault(categoryId, 0);
            likes -= own.values().stream().mapToInt(Integer::intValue).sum();
        }
        double baseShare = (double) categoryLikes.getOrDefault(categoryId, 0) / totalLikes;
        if (likes < MIN_HOBBY_LIKES || baseShare <= 0) {
            return null;
        }
        double share = (inCategory + HOBBY_SMOOTHING * baseShare) / (likes + HOBBY_SMOOTHING);
        double lift = share / baseShare;
        double score = Math.max(0, Math.min(1, (lift - 1) / (FULL_LIFT - 1)));
        return new HobbyCategory(hobbyId, categoryId, inCategory, likes, round(share), round(lift), round(score));
    }

    public double hobbyScore(UUID customerId, UUID hobbyId, UUID categoryId) {
        HobbyCategory link = hobbyCategory(customerId, hobbyId, categoryId);
        return link == null ? 0 : link.score();
    }

    // Every learned link with a positive score, strongest first, per hobby.
    public Map<UUID, List<HobbyCategory>> learnedHobbyCategories() {
        Map<UUID, List<HobbyCategory>> result = new LinkedHashMap<>();
        for (UUID hobbyId : hobbyLikes.keySet()) {
            List<HobbyCategory> links = new ArrayList<>();
            for (UUID categoryId : categoryLikes.keySet()) {
                HobbyCategory link = hobbyCategory(null, hobbyId, categoryId);
                if (link != null && link.score() > 0) {
                    links.add(link);
                }
            }
            if (!links.isEmpty()) {
                links.sort(Comparator.comparingDouble(HobbyCategory::score).reversed().thenComparing(HobbyCategory::categoryId));
                result.put(hobbyId, links);
            }
        }
        return result;
    }

    public int hobbyLikes(UUID hobbyId) {
        return hobbyLikes.getOrDefault(hobbyId, 0);
    }

    public record HobbyCategory(UUID hobbyId, UUID categoryId, int likesInCategory, int likes,
                                double share, double lift, double score) {
    }

    private static double round(double value) {
        return Math.round(value * 10_000) / 10_000.0;
    }
}
