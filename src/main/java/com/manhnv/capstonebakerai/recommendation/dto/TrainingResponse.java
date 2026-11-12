package com.manhnv.capstonebakerai.recommendation.dto;

import com.manhnv.capstonebakerai.recommendation.engine.RecommendationData.Metrics;
import com.manhnv.capstonebakerai.recommendation.engine.RecommendationData.Weights;

import java.util.UUID;

// Result of POST /api/v1/admin/recommendations/models/train.
// baseline = rating + popularity only, defaultWeights = configured weights, trained = learned weights.
// current = the weights customers had when training ran (the active model, or the default weights);
// comparedWithVersion = that model's version, null when it was the default weights.
// active = true when the learned weights were strictly better than current and were switched on.
public record TrainingResponse(
        UUID modelId,
        int modelVersion,
        boolean active,
        int trainingSamples,
        int positiveSamples,
        int testCustomers,
        int k,
        double finalLoss,
        Weights weights,
        Metrics baseline,
        Metrics defaultWeights,
        Metrics trained,
        Metrics current,
        Integer comparedWithVersion,
        String note
) {
}
