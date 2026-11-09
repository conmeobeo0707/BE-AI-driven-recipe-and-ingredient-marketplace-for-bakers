package com.manhnv.capstonebakerai.recommendation.knowledge.entity;

import com.manhnv.capstonebakerai.customer.enums.SkillLevel;
import com.manhnv.capstonebakerai.recipe.enums.DifficultyEnum;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

// Knowledge: how well a recipe difficulty suits a skill level (0 = not suitable, 1 = ideal).
@Entity
@Table(name = "skill_difficulty_fit")
@IdClass(SkillDifficultyFitId.class)
@Getter
@Setter
@NoArgsConstructor
public class SkillDifficultyFit {

    @Id
    @Enumerated(EnumType.STRING)
    @Column(name = "skill_level", nullable = false, length = 20)
    private SkillLevel skillLevel;

    @Id
    @Enumerated(EnumType.STRING)
    @Column(name = "difficulty", nullable = false, length = 20)
    private DifficultyEnum difficulty;

    @Column(name = "fit", nullable = false, precision = 3, scale = 2)
    private BigDecimal fit;
}
