package com.manhnv.capstonebakerai.recipe.entity;

import com.manhnv.capstonebakerai.recipe.enums.InteractionSource;
import com.manhnv.capstonebakerai.recipe.enums.RecipeInteractionType;
import com.manhnv.capstonebakerai.customer.entity.Customer;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "recipe_interactions")
@Getter
@Setter
@NoArgsConstructor
public class RecipeInteraction {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "customer_id", nullable = false)
    private Customer customer;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "recipe_id")
    private Recipe recipe;

    @Enumerated(EnumType.STRING)
    @Column(name = "interaction_type", nullable = false, length = 20)
    private RecipeInteractionType interactionType;

    @Column(name = "rating_score")
    private Integer ratingScore;

    @Column(name = "recomm_id", length = 100)
    private String recommId;

    @Column(name = "search_query", length = 255)
    private String searchQuery;

    @Enumerated(EnumType.STRING)
    @Column(name = "source", nullable = false, length = 20)
    private InteractionSource source = InteractionSource.DIRECT;

    @Column(name = "occurred_at", nullable = false)
    private Instant occurredAt = Instant.now();
}
