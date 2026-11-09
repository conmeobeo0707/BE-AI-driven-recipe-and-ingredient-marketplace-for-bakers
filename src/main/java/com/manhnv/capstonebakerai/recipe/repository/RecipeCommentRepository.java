package com.manhnv.capstonebakerai.recipe.repository;

import com.manhnv.capstonebakerai.recipe.entity.RecipeComment;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface RecipeCommentRepository extends JpaRepository<RecipeComment, UUID> {
    List<RecipeComment> findByRecipe_Id(UUID recipeId);

    Optional<RecipeComment> findById(UUID id);

    Page<RecipeComment> findByContentContainingIgnoreCase(String content, Pageable pageable);
    Page<RecipeComment> findByRecipe_IdAndContentContainingIgnoreCase(UUID recipeId, String content, Pageable pageable);
    Page<RecipeComment> findByUser_UserIdAndContentContainingIgnoreCase(UUID userId, String content, Pageable pageable);
    Page<RecipeComment> findByParent_IdAndContentContainingIgnoreCase(UUID parentId, String content, Pageable pageable);

    Page<RecipeComment> findByRecipe_Id(UUID recipeId, Pageable pageable);
    Page<RecipeComment> findByUser_UserId(UUID userId, Pageable pageable);
    Page<RecipeComment> findByParent_Id(UUID parentId, Pageable pageable);
}
