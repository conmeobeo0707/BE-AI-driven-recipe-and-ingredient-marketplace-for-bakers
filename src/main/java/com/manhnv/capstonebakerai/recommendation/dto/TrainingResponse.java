package com.manhnv.capstonebakerai.recommendation.dto;

import com.manhnv.capstonebakerai.recommendation.engine.RecommendationData.Metrics;
import com.manhnv.capstonebakerai.recommendation.engine.RecommendationData.Weights;

import java.util.UUID;

// Result of POST /api/v1/admin/recommendations/models/train.
// baseline = rating + popularity only, defaultWeights = configured weights, trained = learned weights.
// active = true when the learned weights beat the default weights and were switched on.
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
        String note
) {
}
