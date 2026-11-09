package com.manhnv.capstonebakerai.recommendation.knowledge.entity;

import com.manhnv.capstonebakerai.customer.enums.SkillLevel;
import com.manhnv.capstonebakerai.recipe.enums.DifficultyEnum;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode
public class SkillDifficultyFitId implements Serializable {
    private SkillLevel skillLevel;
    private DifficultyEnum difficulty;
}
