package com.manhnv.capstonebakerai.learner.entity;

import com.manhnv.capstonebakerai.identity.entity.User;
import com.manhnv.capstonebakerai.identity.enums.SkillLevel;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "learners")
@Getter
@Setter
@NoArgsConstructor
public class Learner {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "learner_id", nullable = false, updatable = false)
    private UUID learnerId;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(name = "skill_level", length = 20)
    private SkillLevel skillLevel;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;
}
