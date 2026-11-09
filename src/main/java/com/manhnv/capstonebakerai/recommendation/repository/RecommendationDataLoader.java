package com.manhnv.capstonebakerai.recommendation.repository;

import com.manhnv.capstonebakerai.customer.enums.SkillLevel;
import com.manhnv.capstonebakerai.recipe.enums.DifficultyEnum;
import com.manhnv.capstonebakerai.recipe.enums.RecipeInteractionType;
import com.manhnv.capstonebakerai.recipe.enums.RecipeStatus;
import com.manhnv.capstonebakerai.recommendation.engine.RecommendationData.*;
import com.manhnv.capstonebakerai.recommendation.knowledge.entity.SkillDifficultyFit;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.TypedQuery;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.*;

// Reads what the recommender needs with a few small queries (only the needed columns, no N+1).
// Likes come from the bookmarks and ratings tables (current state, timed by their newest history row),
// views from recipe_interactions.
@Repository
@Transactional(readOnly = true)
public class RecommendationDataLoader {

    private static final Duration CUSTOMER_HISTORY = Duration.ofDays(180);
    private static final int MAX_CUSTOMER_VIEWS = 500;

    @PersistenceContext
    private EntityManager entityManager;

    // Everything, for scoring (shared parts) and for training/evaluation.
    public Snapshot loadSnapshot() {
        List<Signal> signals = new ArrayList<>();
        signals.addAll(bookmarks(null));
        signals.addAll(ratings(null));
        signals.addAll(views(null, null, Integer.MAX_VALUE));

        Map<UUID, SkillLevel> skillLevels = new HashMap<>();
        for (Object[] row : entityManager.createQuery("select c.id, c.skillLevel from Customer c", Object[].class)
                .getResultList()) {
            skillLevels.put((UUID) row[0], (SkillLevel) row[1]);
        }
        return Snapshot.of(publishedRecipes(), signals, skillLevels, activeHobbies(null),
                skillFit(), hobbyNames(), Instant.now());
    }

    // One customer, freshly read, so a bookmark made a second ago already counts.
    public CustomerData loadCustomer(UUID customerId) {
        List<Signal> signals = new ArrayList<>();
        signals.addAll(bookmarks(customerId));
        signals.addAll(ratings(customerId));
        signals.addAll(views(customerId, Instant.now().minus(CUSTOMER_HISTORY), MAX_CUSTOMER_VIEWS));
        signals.sort(Comparator.comparing(Signal::at));
        SkillLevel skillLevel = entityManager.createQuery(
                        "select c.skillLevel from Customer c where c.id = :id", SkillLevel.class)
                .setParameter("id", customerId)
                .getResultStream().findFirst().orElse(null);
        return new CustomerData(customerId, skillLevel,
                activeHobbies(customerId).getOrDefault(customerId, Set.of()), signals);
    }

    public Map<SkillLevel, Map<DifficultyEnum, Double>> skillFit() {
        Map<SkillLevel, Map<DifficultyEnum, Double>> fit = new EnumMap<>(SkillLevel.class);
        for (SkillDifficultyFit row : entityManager.createQuery(
                "select f from SkillDifficultyFit f", SkillDifficultyFit.class).getResultList()) {
            fit.computeIfAbsent(row.getSkillLevel(), level -> new EnumMap<>(DifficultyEnum.class))
                    .put(row.getDifficulty(), row.getFit().doubleValue());
        }
        return fit;
    }

    private Map<UUID, String> hobbyNames() {
        Map<UUID, String> names = new HashMap<>();
        for (Object[] row : entityManager.createQuery(
                "select h.id, h.name from Hobby h where h.active = true", Object[].class).getResultList()) {
            names.put((UUID) row[0], (String) row[1]);
        }
        return names;
    }

    private List<RecipeItem> publishedRecipes() {
        // Only ingredients linked to an active catalog entry count towards taste.
        Map<UUID, Map<UUID, String>> ingredients = new HashMap<>();
        for (Object[] row : entityManager.createQuery("""
                select i.recipe.id, c.id, c.canonicalName
                from Ingredient i join i.catalogIngredient c
                where i.recipe.recipeStatus = :status and c.active = true
                """, Object[].class)
                .setParameter("status", RecipeStatus.PUBLISHED)
                .getResultList()) {
            ingredients.computeIfAbsent((UUID) row[0], id -> new LinkedHashMap<>()).put((UUID) row[1], (String) row[2]);
        }
        List<RecipeItem> recipes = new ArrayList<>();
        for (Object[] row : entityManager.createQuery("""
                select r.id, r.title, c.id, c.name, r.difficulty, r.createdAt
                from Recipe r left join r.recipeCategory c
                where r.recipeStatus = :status
                """, Object[].class)
                .setParameter("status", RecipeStatus.PUBLISHED)
                .getResultList()) {
            recipes.add(new RecipeItem((UUID) row[0], (String) row[1], (UUID) row[2], (String) row[3],
                    (DifficultyEnum) row[4], toInstant((LocalDateTime) row[5], null),
                    Map.copyOf(ingredients.getOrDefault((UUID) row[0], Map.of()))));
        }
        return recipes;
    }

    // The time of a bookmark/rating is when it was last made or changed: the newest matching history row,
    // falling back to the row's created_at (which never changes, even when a rating is edited).
    private List<Signal> bookmarks(UUID customerId) {
        return query("select b.customer.id, b.recipe.id, b.createdAt, (" + latestHistoryTime("b") + ")"
                + " from RecipeBookmark b"
                + (customerId == null ? "" : " where b.customer.id = :customerId"), customerId)
                .setParameter("historyType", RecipeInteractionType.BOOKMARK)
                .getResultList().stream()
                .map(row -> new Signal((UUID) row[0], (UUID) row[1], SignalType.BOOKMARK, 0,
                        lastChanged((LocalDateTime) row[2], (Instant) row[3])))
                .toList();
    }

    private List<Signal> ratings(UUID customerId) {
        return query("select r.customer.id, r.recipe.id, r.score, r.createdAt, (" + latestHistoryTime("r") + ")"
                + " from RecipeRating r"
                + (customerId == null ? "" : " where r.customer.id = :customerId"), customerId)
                .setParameter("historyType", RecipeInteractionType.RATING)
                .getResultList().stream()
                .filter(row -> row[2] != null)
                .map(row -> new Signal((UUID) row[0], (UUID) row[1], SignalType.RATING, (Integer) row[2],
                        lastChanged((LocalDateTime) row[3], (Instant) row[4])))
                .toList();
    }

    private static String latestHistoryTime(String alias) {
        return "select max(h.occurredAt) from RecipeInteraction h where h.customer = " + alias + ".customer"
                + " and h.recipe = " + alias + ".recipe and h.interactionType = :historyType";
    }

    private static Instant lastChanged(LocalDateTime createdAt, Instant latestHistory) {
        Instant created = toInstant(createdAt, Instant.EPOCH);
        return latestHistory != null && latestHistory.isAfter(created) ? latestHistory : created;
    }

    private List<Signal> views(UUID customerId, Instant since, int max) {
        TypedQuery<Object[]> query = query("select i.customer.id, i.recipe.id, i.occurredAt from RecipeInteraction i"
                + " where i.interactionType = :type"
                + (customerId == null ? "" : " and i.customer.id = :customerId")
                + (since == null ? "" : " and i.occurredAt >= :since")
                + " order by i.occurredAt desc", customerId)
                .setParameter("type", RecipeInteractionType.VIEW)
                .setMaxResults(max);
        if (since != null) {
            query.setParameter("since", since);
        }
        return query.getResultList().stream()
                .map(row -> new Signal((UUID) row[0], (UUID) row[1], SignalType.VIEW, 0, (Instant) row[2]))
                .toList();
    }

    // customer -> active hobbies
    private Map<UUID, Set<UUID>> activeHobbies(UUID customerId) {
        Map<UUID, Set<UUID>> result = new HashMap<>();
        for (Object[] row : query("select ch.customerId, ch.hobbyId from CustomerHobby ch, Hobby h"
                + " where h.id = ch.hobbyId and h.active = true"
                + (customerId == null ? "" : " and ch.customerId = :customerId"), customerId).getResultList()) {
            result.computeIfAbsent((UUID) row[0], id -> new HashSet<>()).add((UUID) row[1]);
        }
        return result;
    }

    private TypedQuery<Object[]> query(String jpql, UUID customerId) {
        TypedQuery<Object[]> query = entityManager.createQuery(jpql, Object[].class);
        if (customerId != null) {
            query.setParameter("customerId", customerId);
        }
        return query;
    }

    // LocalDateTime columns are written by the app in its own time zone. Rows without a time count as very old.
    private static Instant toInstant(LocalDateTime value, Instant whenMissing) {
        return value == null ? whenMissing : value.atZone(ZoneId.systemDefault()).toInstant();
    }
}
