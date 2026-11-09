package com.manhnv.capstonebakerai.recommendation.dto;

import com.manhnv.capstonebakerai.customer.enums.SkillLevel;
import com.manhnv.capstonebakerai.recipe.enums.DifficultyEnum;
import com.manhnv.capstonebakerai.recommendation.engine.RecommendationData.Weights;

import java.util.List;

// The recommender's knowledge base at a glance (GET /api/v1/admin/recommendations/knowledge):
// stored knowledge (ingredient catalog, skill/difficulty table), learned knowledge (hobby -> category)
// and the weights in use.
public record KnowledgeResponse(
        long activeCatalogIngredients,
        long ingredientAliases,
        double linkedIngredientRatio,      // published recipes' ingredient rows linked to the catalog
        long publishedRecipes,
        long likes,                        // bookmarks + 4-5 star ratings
        List<SkillFit> skillFit,
        List<LearnedHobby> learnedHobbies,
        Integer activeModelVersion,        // null = default weights
        Weights weightsInUse
) {
    public record SkillFit(SkillLevel skillLevel, DifficultyEnum difficulty, double fit) {
    }

    // likes: likes of the customers holding the hobby; categories: what they like more than average.
    public record LearnedHobby(String hobby, int likes, List<LearnedCategory> categories) {
    }

    // lift: how many times more than average; score (0..1): strength used when recommending.
    public record LearnedCategory(String category, int likes, double lift, double score) {
    }
}
