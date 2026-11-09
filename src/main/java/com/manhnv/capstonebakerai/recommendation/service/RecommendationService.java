package com.manhnv.capstonebakerai.recommendation.service;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.manhnv.capstonebakerai.common.exception.AppException;
import com.manhnv.capstonebakerai.customer.exception.CustomerErrorCode;
import com.manhnv.capstonebakerai.customer.repository.CustomerRepository;
import com.manhnv.capstonebakerai.recipe.entity.Recipe;
import com.manhnv.capstonebakerai.recipe.enums.RecipeStatus;
import com.manhnv.capstonebakerai.recipe.mapper.RecipeMapper;
import com.manhnv.capstonebakerai.recipe.repository.RecipeRepository;
import com.manhnv.capstonebakerai.recommendation.config.RecommendationProperties;
import com.manhnv.capstonebakerai.recommendation.dto.RecipeRecommendationResponse;
import com.manhnv.capstonebakerai.recommendation.engine.CommunityStats;
import com.manhnv.capstonebakerai.recommendation.engine.RecommendationData.CustomerData;
import com.manhnv.capstonebakerai.recommendation.engine.RecommendationData.ScoredRecipe;
import com.manhnv.capstonebakerai.recommendation.engine.RecommendationData.Snapshot;
import com.manhnv.capstonebakerai.recommendation.engine.RecommendationData.Weights;
import com.manhnv.capstonebakerai.recommendation.engine.Scorer;
import com.manhnv.capstonebakerai.recommendation.event.CustomerProfileChangedEvent;
import com.manhnv.capstonebakerai.recommendation.event.RecipeInteractionDeletedEvent;
import com.manhnv.capstonebakerai.recommendation.event.RecipeInteractionRecordedEvent;
import com.manhnv.capstonebakerai.recommendation.knowledge.entity.RecommendationModel;
import com.manhnv.capstonebakerai.recommendation.knowledge.repository.RecommendationModelRepository;
import com.manhnv.capstonebakerai.recommendation.repository.RecommendationDataLoader;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

// Recommendations for the logged-in customer.
//
// Two in-memory caches (5 minutes each):
// - the shared snapshot (recipes, everyone's signals, knowledge base) + community statistics;
// - each customer's ranked list, dropped as soon as that customer views, bookmarks, rates or edits their profile.
@Service
@RequiredArgsConstructor
public class RecommendationService {

    public static final int MAX_COUNT = 20;
    private static final Duration CACHE_TIME = Duration.ofMinutes(5);

    private final CustomerRepository customerRepository;
    private final RecipeRepository recipeRepository;
    private final RecommendationDataLoader dataLoader;
    private final RecommendationModelRepository modelRepository;
    private final RecommendationProperties properties;
    private final Scorer scorer;

    private final Cache<String, Shared> sharedCache = Caffeine.newBuilder().maximumSize(1).expireAfterWrite(CACHE_TIME).build();
    private final Cache<UUID, Ranked> rankedCache = Caffeine.newBuilder().maximumSize(10_000).expireAfterWrite(CACHE_TIME).build();

    private record Shared(Snapshot snapshot, CommunityStats stats) {
    }

    private record Ranked(String recommId, Integer modelVersion, List<ScoredRecipe> recipes) {
    }

    @Transactional(readOnly = true)
    public RecipeRecommendationResponse recommend(UUID userId, int count) {
        UUID customerId = customerRepository.findByUser_UserId(userId)
                .orElseThrow(() -> new AppException(CustomerErrorCode.CUSTOMER_PROFILE_NOT_FOUND))
                .getId();
        Ranked ranked = rankedCache.get(customerId, this::rank);

        // Recipes are read again so one hidden after the list was cached is left out.
        List<ScoredRecipe> top = ranked.recipes().stream().limit(count).toList();
        Map<UUID, Recipe> recipes = recipeRepository
                .findByIdInAndRecipeStatus(top.stream().map(ScoredRecipe::recipeId).toList(), RecipeStatus.PUBLISHED)
                .stream().collect(Collectors.toMap(Recipe::getId, Function.identity()));
        List<RecipeRecommendationResponse.Item> items = top.stream()
                .filter(item -> recipes.containsKey(item.recipeId()))
                .map(item -> new RecipeRecommendationResponse.Item(
                        RecipeMapper.toDTO(recipes.get(item.recipeId())), item.score(), item.reasons()))
                .toList();
        return new RecipeRecommendationResponse(ranked.modelVersion(), ranked.recommId(), items);
    }

    // Ranks the top 20 once; later requests within 5 minutes only slice this list.
    private Ranked rank(UUID customerId) {
        Shared shared = shared();
        CustomerData customer = dataLoader.loadCustomer(customerId);
        Optional<RecommendationModel> model = modelRepository.findFirstByActiveTrue();
        Weights weights = model.map(RecommendationModel::weights).orElseGet(properties::defaultWeights);
        Scorer.Context context = scorer.prepare(shared.snapshot(), shared.stats(), customer, Instant.now());
        return new Ranked(UUID.randomUUID().toString(), model.map(RecommendationModel::getVersion).orElse(null),
                scorer.rank(context, weights, MAX_COUNT, true));
    }

    private Shared shared() {
        return sharedCache.get("shared", key -> {
            Snapshot snapshot = dataLoader.loadSnapshot();
            return new Shared(snapshot, CommunityStats.compute(snapshot, snapshot.loadedAt()));
        });
    }

    // Community statistics currently used for scoring (for the admin knowledge view).
    public CommunityStats currentStats() {
        return shared().stats();
    }

    public Snapshot currentSnapshot() {
        return shared().snapshot();
    }

    // A new model was activated: rebuild everything.
    public void clearAll() {
        sharedCache.invalidateAll();
        rankedCache.invalidateAll();
    }

    // ---------- the customer did something new: drop their cached list ----------

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    public void onInteractionRecorded(RecipeInteractionRecordedEvent event) {
        rankedCache.invalidate(event.customerId());
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    public void onInteractionDeleted(RecipeInteractionDeletedEvent event) {
        rankedCache.invalidate(event.customerId());
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    public void onProfileChanged(CustomerProfileChangedEvent event) {
        rankedCache.invalidate(event.customerId());
    }
}
