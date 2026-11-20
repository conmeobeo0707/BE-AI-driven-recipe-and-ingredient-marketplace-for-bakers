package com.manhnv.capstonebakerai.recommendation.knowledge.entity;

import com.manhnv.capstonebakerai.recommendation.engine.RecommendationData.Metrics;
import com.manhnv.capstonebakerai.recommendation.engine.RecommendationData.Weights;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

// One training run: the learned feature weights and the offline evaluation that justified them.
// At most one row is active; its weights are used for live recommendations.
@Entity
@Table(name = "recommendation_models")
@Getter
@Setter
@NoArgsConstructor
public class RecommendationModel {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "version", nullable = false, unique = true)
    private int version;

    @Column(name = "weight_content", nullable = false)
    private double weightContent;

    @Column(name = "weight_skill", nullable = false)
    private double weightSkill;

    @Column(name = "weight_hobby", nullable = false)
    private double weightHobby;

    @Column(name = "weight_collaborative", nullable = false)
    private double weightCollaborative;

    @Column(name = "weight_quality", nullable = false)
    private double weightQuality;

    @Column(name = "training_samples", nullable = false)
    private int trainingSamples;

    @Column(name = "positive_samples", nullable = false)
    private int positiveSamples;

    @Column(name = "test_customers", nullable = false)
    private int testCustomers;

    @Column(name = "eval_k", nullable = false)
    private int evalK;

    @Column(name = "baseline_hit_rate", nullable = false)
    private double baselineHitRate;

    @Column(name = "baseline_mrr", nullable = false)
    private double baselineMrr;

    @Column(name = "baseline_coverage", nullable = false)
    private double baselineCoverage;

    @Column(name = "default_hit_rate", nullable = false)
    private double defaultHitRate;

    @Column(name = "default_mrr", nullable = false)
    private double defaultMrr;

    @Column(name = "default_coverage", nullable = false)
    private double defaultCoverage;

    @Column(name = "trained_hit_rate", nullable = false)
    private double trainedHitRate;

    @Column(name = "trained_mrr", nullable = false)
    private double trainedMrr;

    @Column(name = "trained_coverage", nullable = false)
    private double trainedCoverage;

    @Column(name = "final_loss", nullable = false)
    private double finalLoss;

    @Column(name = "is_active", nullable = false)
    private boolean active;

    @Column(name = "trained_at", nullable = false)
    private Instant trainedAt;

    @Column(name = "trained_by")
    private UUID trainedBy;

    @Column(name = "activated_at")
    private Instant activatedAt;

    public Weights weights() {
        return new Weights(weightContent, weightSkill, weightHobby, weightCollaborative, weightQuality);
    }

    public void setWeights(Weights weights) {
        this.weightContent = weights.content();
        this.weightSkill = weights.skill();
        this.weightHobby = weights.hobby();
        this.weightCollaborative = weights.collaborative();
        this.weightQuality = weights.quality();
    }

    public void setMetrics(Metrics baseline, Metrics defaults, Metrics trained) {
        this.baselineHitRate = baseline.hitRate();
        this.baselineMrr = baseline.mrr();
        this.baselineCoverage = baseline.coverage();
        this.defaultHitRate = defaults.hitRate();
        this.defaultMrr = defaults.mrr();
        this.defaultCoverage = defaults.coverage();
        this.trainedHitRate = trained.hitRate();
        this.trainedMrr = trained.mrr();
        this.trainedCoverage = trained.coverage();
    }
}
