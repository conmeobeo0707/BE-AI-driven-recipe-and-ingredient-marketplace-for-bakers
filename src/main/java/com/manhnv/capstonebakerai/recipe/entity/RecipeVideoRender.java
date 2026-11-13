package com.manhnv.capstonebakerai.recipe.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "recipe_video_renders", indexes = @Index(name = "idx_recipe_video_renders_render_id", columnList = "render_id", unique = true))
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RecipeVideoRender {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "recipe_id", nullable = false)
    private UUID recipeId;

    @Column(name = "render_id", nullable = false, unique = true)
    private String renderId;

    @Column(nullable = false, length = 32)
    private String status;

    @Column(name = "video_url", length = 2048)
    private String videoUrl;

    @Column(name = "error_message", length = 2000)
    private String errorMessage;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "completed_at")
    private Instant completedAt;

    @PrePersist
    void onCreate() {
        if (createdAt == null) createdAt = Instant.now();
    }
}
