package com.manhnv.capstonebakerai.recommendation.service;

import com.manhnv.capstonebakerai.common.exception.AppException;
import com.manhnv.capstonebakerai.recipe.enums.RecipeStatus;
import com.manhnv.capstonebakerai.recommendation.config.RecommendationProperties;
import com.manhnv.capstonebakerai.recommendation.dto.KnowledgeResponse;
import com.manhnv.capstonebakerai.recommendation.dto.TrainingResponse;
import com.manhnv.capstonebakerai.recommendation.engine.CommunityStats;
import com.manhnv.capstonebakerai.recommendation.engine.LogisticRegression;
import com.manhnv.capstonebakerai.recommendation.engine.RecommendationData.*;
import com.manhnv.capstonebakerai.recommendation.engine.Scorer;
import com.manhnv.capstonebakerai.recommendation.exception.RecommendationErrorCode;
import com.manhnv.capstonebakerai.recommendation.knowledge.entity.RecommendationModel;
import com.manhnv.capstonebakerai.recommendation.knowledge.repository.RecommendationModelRepository;
import com.manhnv.capstonebakerai.recommendation.repository.RecommendationDataLoader;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.Instant;
import java.util.*;

// What the ADMIN does: train the model (and activate it when it is better) and look at the knowledge base.
//
// Training in 4 steps:
// 1. Split: for each customer with 3+ likes, hide the LAST like as the test case; the other likes are for learning.
// 2. Samples: each learning like is a positive (label 1) paired with 4 random recipes the customer never liked
//    (label 0). Features are computed only from data that existed just before that like (no peeking ahead).
// 3. Learn: logistic regression gives one weight per feature (negatives cut to 0, scaled to sum to 1).
// 4. Evaluate on the hidden likes: is the liked recipe in the top k? (Hit@k, MRR, coverage), for
//    baseline (rating + popularity only), the default weights and the learned weights.
//    The learned weights become active when they beat the default weights.
@Service
@RequiredArgsConstructor
@Slf4j
public class RecommendationAdminService {

    private static final int MIN_LIKES_TO_TEST = 3;
    private static final int NEGATIVES_PER_POSITIVE = 4;
    private static final long RANDOM_SEED = 42;

    @PersistenceContext
    private EntityManager entityManager;

    private final RecommendationDataLoader dataLoader;
    private final RecommendationModelRepository modelRepository;
    private final RecommendationProperties properties;
    private final RecommendationService recommendationService;
    private final Scorer scorer;
    private final TransactionTemplate transactionTemplate;

    private record Like(UUID customerId, UUID recipeId, Instant at) {
    }

    // synchronized: two admins pressing "train" at once simply run one after the other.
    public synchronized TrainingResponse train(int k, UUID adminUserId) {
        if (k < 1 || k > 20) {
            throw new AppException(RecommendationErrorCode.INVALID_K);
        }
        Snapshot snapshot = dataLoader.loadSnapshot();

        // 1. Split
        List<Like> learnLikes = new ArrayList<>();
        List<Like> testLikes = new ArrayList<>();
        for (CustomerData customer : snapshot.customers().values()) {
            List<Like> likes = likes(customer, snapshot);
            if (likes.size() >= MIN_LIKES_TO_TEST) {
                learnLikes.addAll(likes.subList(0, likes.size() - 1));
                testLikes.add(likes.get(likes.size() - 1));
            } else {
                learnLikes.addAll(likes);
            }
        }
        RecommendationProperties.Training minimum = properties.getTraining();
        if (testLikes.size() < minimum.getMinTestCustomers() || learnLikes.size() < minimum.getMinPositiveSamples()) {
            throw new AppException(RecommendationErrorCode.NOT_ENOUGH_TRAINING_DATA, String.format(
                    "Not enough data to train: need %d customers with 3 or more liked recipes (have %d) and %d liked "
                            + "recipes to learn from (have %d). A like is a bookmark or a 4-5 star rating.",
                    minimum.getMinTestCustomers(), testLikes.size(), minimum.getMinPositiveSamples(), learnLikes.size()));
        }

        // 2. Samples
        List<LogisticRegression.Sample> samples = samples(snapshot, learnLikes);
        int positives = (int) samples.stream().filter(sample -> sample.label() == 1).count();

        // 3. Learn
        LogisticRegression.Result result = LogisticRegression.train(samples);
        Weights learned = Weights.fromArray(result.weights()).normalized();
        if (learned == null) {
            throw new AppException(RecommendationErrorCode.TRAINING_FAILED);
        }

        // 4. Evaluate
        Weights defaults = properties.defaultWeights();
        List<Metrics> metrics = evaluate(snapshot, testLikes, k,
                List.of(Weights.qualityOnly(), defaults, learned));
        Metrics baseline = metrics.get(0);
        Metrics defaultMetrics = metrics.get(1);
        Metrics trained = metrics.get(2);
        boolean better = trained.hitRate() > defaultMetrics.hitRate()
                || (trained.hitRate() == defaultMetrics.hitRate() && trained.mrr() >= defaultMetrics.mrr());

        RecommendationModel model = transactionTemplate.execute(status -> {
            RecommendationModel saved = new RecommendationModel();
            saved.setVersion(modelRepository.findMaxVersion() + 1);
            saved.setWeights(learned);
            saved.setTrainingSamples(samples.size());
            saved.setPositiveSamples(positives);
            saved.setTestCustomers(testLikes.size());
            saved.setEvalK(k);
            saved.setMetrics(baseline, defaultMetrics, trained);
            saved.setFinalLoss(Math.round(result.loss() * 1_000_000) / 1_000_000.0);
            saved.setTrainedAt(Instant.now());
            saved.setTrainedBy(adminUserId);
            if (better) {
                modelRepository.deactivateAll();
                saved.setActive(true);
                saved.setActivatedAt(Instant.now());
            }
            return modelRepository.saveAndFlush(saved);
        });
        if (better) {
            recommendationService.clearAll();
        }
        log.info("Trained recommendation model v{} on {} samples ({} iterations, loss {}), active={}",
                model.getVersion(), samples.size(), result.iterations(), result.loss(), better);

        return new TrainingResponse(model.getId(), model.getVersion(), better, samples.size(), positives,
                testLikes.size(), k, model.getFinalLoss(), learned, baseline, defaultMetrics, trained,
                better ? "Model mới tốt hơn trọng số mặc định nên đã được kích hoạt."
                        : "Model mới không tốt hơn trọng số mặc định nên chưa được kích hoạt; hệ thống giữ cấu hình đang dùng.");
    }

    // First like of each published recipe, oldest first (likes dated before the recipe existed are ignored).
    private static List<Like> likes(CustomerData customer, Snapshot snapshot) {
        Map<UUID, Like> first = new LinkedHashMap<>();
        for (Signal signal : customer.signals()) {
            RecipeItem recipe = snapshot.recipesById().get(signal.recipeId());
            if (signal.liked() && recipe != null && !Instant.EPOCH.equals(signal.at())
                    && (recipe.createdAt() == null || recipe.createdAt().isBefore(signal.at()))) {
                first.putIfAbsent(signal.recipeId(), new Like(customer.id(), signal.recipeId(), signal.at()));
            }
        }
        return new ArrayList<>(first.values());
    }

    private List<LogisticRegression.Sample> samples(Snapshot snapshot, List<Like> likes) {
        Random random = new Random(RANDOM_SEED);
        List<Like> ordered = new ArrayList<>(likes);
        ordered.sort(Comparator.comparing(Like::customerId).thenComparing(Like::at));
        List<LogisticRegression.Sample> samples = new ArrayList<>();
        for (Like like : ordered) {
            Scorer.Context context = contextBefore(snapshot, like);
            List<RecipeItem> candidates = scorer.candidates(context);
            RecipeItem target = snapshot.recipesById().get(like.recipeId());
            if (!candidates.contains(target)) {
                continue;
            }
            samples.add(new LogisticRegression.Sample(scorer.features(target, context).toArray(), 1));
            Set<UUID> everLiked = new HashSet<>();
            snapshot.customer(like.customerId()).signals().stream()
                    .filter(Signal::liked).forEach(signal -> everLiked.add(signal.recipeId()));
            List<RecipeItem> pool = new ArrayList<>(candidates.stream()
                    .filter(recipe -> !everLiked.contains(recipe.id())).toList());
            for (int i = 0; i < NEGATIVES_PER_POSITIVE && !pool.isEmpty(); i++) {
                RecipeItem negative = pool.remove(random.nextInt(pool.size()));
                samples.add(new LogisticRegression.Sample(scorer.features(negative, context).toArray(), 0));
            }
        }
        if (samples.stream().noneMatch(sample -> sample.label() == 1)) {
            throw new AppException(RecommendationErrorCode.TRAINING_FAILED);
        }
        return samples;
    }

    // For every hidden like, rank the top k with each weight set and check whether the liked recipe is there.
    private List<Metrics> evaluate(Snapshot snapshot, List<Like> testLikes, int k, List<Weights> variants) {
        double[] hits = new double[variants.size()];
        double[] reciprocalRanks = new double[variants.size()];
        List<Set<UUID>> shown = new ArrayList<>();
        variants.forEach(variant -> shown.add(new HashSet<>()));
        for (Like like : testLikes) {
            Scorer.Context context = contextBefore(snapshot, like);
            for (int v = 0; v < variants.size(); v++) {
                List<ScoredRecipe> top = scorer.rank(context, variants.get(v), k, false);
                for (int position = 0; position < top.size(); position++) {
                    shown.get(v).add(top.get(position).recipeId());
                    if (top.get(position).recipeId().equals(like.recipeId())) {
                        hits[v]++;
                        reciprocalRanks[v] += 1.0 / (position + 1);
                    }
                }
            }
        }
        List<Metrics> metrics = new ArrayList<>();
        for (int v = 0; v < variants.size(); v++) {
            metrics.add(new Metrics(
                    round(hits[v] / testLikes.size()),
                    round(reciprocalRanks[v] / testLikes.size()),
                    round((double) shown.get(v).size() / Math.max(1, snapshot.recipes().size()))));
        }
        return metrics;
    }

    // The customer and the whole community exactly as they were just before the like.
    private Scorer.Context contextBefore(Snapshot snapshot, Like like) {
        return scorer.prepare(snapshot, CommunityStats.compute(snapshot, like.at()),
                snapshot.customer(like.customerId()).before(like.at()), like.at());
    }

    // ---------- knowledge base overview ----------

    @Transactional(readOnly = true)
    public KnowledgeResponse knowledge() {
        Snapshot snapshot = recommendationService.currentSnapshot();
        CommunityStats stats = recommendationService.currentStats();

        List<KnowledgeResponse.SkillFit> skillFit = new ArrayList<>();
        dataLoader.skillFit().forEach((skill, row) -> row.forEach((difficulty, fit) ->
                skillFit.add(new KnowledgeResponse.SkillFit(skill, difficulty, fit))));

        Map<UUID, String> categoryNames = new HashMap<>();
        snapshot.recipes().forEach(recipe -> categoryNames.putIfAbsent(recipe.categoryId(), recipe.categoryName()));
        List<KnowledgeResponse.LearnedHobby> hobbies = new ArrayList<>();
        Map<UUID, List<CommunityStats.HobbyCategory>> learned = stats.learnedHobbyCategories();
        snapshot.hobbyNames().forEach((hobbyId, hobbyName) -> hobbies.add(new KnowledgeResponse.LearnedHobby(
                hobbyName, stats.hobbyLikes(hobbyId),
                learned.getOrDefault(hobbyId, List.of()).stream()
                        .map(link -> new KnowledgeResponse.LearnedCategory(categoryNames.get(link.categoryId()),
                                link.likesInCategory(), link.lift(), link.score()))
                        .toList())));
        hobbies.sort(Comparator.comparingInt(KnowledgeResponse.LearnedHobby::likes).reversed());

        long ingredientRows = count("select count(i) from Ingredient i join i.recipe r where r.recipeStatus = :published");
        long linkedRows = count("select count(i) from Ingredient i join i.recipe r where r.recipeStatus = :published"
                + " and i.catalogIngredient is not null");
        Optional<RecommendationModel> active = modelRepository.findFirstByActiveTrue();
        return new KnowledgeResponse(
                count("select count(c) from IngredientCatalog c where c.active = true"),
                count("select count(a) from IngredientAlias a"),
                ingredientRows == 0 ? 0 : Math.round((double) linkedRows / ingredientRows * 10_000) / 10_000.0,
                snapshot.recipes().size(),
                snapshot.signals().stream().filter(Signal::liked).count(),
                skillFit,
                hobbies,
                active.map(RecommendationModel::getVersion).orElse(null),
                active.map(RecommendationModel::weights).orElseGet(properties::defaultWeights)
        );
    }

    private long count(String jpql) {
        var query = entityManager.createQuery(jpql, Long.class);
        if (jpql.contains(":published")) {
            query.setParameter("published", RecipeStatus.PUBLISHED);
        }
        return query.getSingleResult();
    }

    private static double round(double value) {
        return Math.round(value * 10_000) / 10_000.0;
    }
}
