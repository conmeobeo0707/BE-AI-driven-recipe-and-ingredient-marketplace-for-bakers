package com.manhnv.capstonebakerai.recipe.ingredientcatalog.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "ingredient_aliases")
@Getter
@Setter
@NoArgsConstructor
public class IngredientAlias {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "ingredient_id", nullable = false)
    private IngredientCatalog ingredient;

    @Column(name = "alias", nullable = false, length = 150)
    private String alias;

    @Column(name = "normalized_alias", nullable = false, unique = true, length = 150)
    private String normalizedAlias;

    @Column(name = "language_code", length = 10)
    private String languageCode;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;
}
