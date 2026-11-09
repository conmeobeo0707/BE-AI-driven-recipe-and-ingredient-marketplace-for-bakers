package com.manhnv.capstonebakerai.recommendation.knowledge.repository;

import com.manhnv.capstonebakerai.recommendation.knowledge.entity.RecommendationModel;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import java.util.Optional;
import java.util.UUID;

public interface RecommendationModelRepository extends JpaRepository<RecommendationModel, UUID> {

    Optional<RecommendationModel> findFirstByActiveTrue();

    @Query("select coalesce(max(m.version), 0) from RecommendationModel m")
    int findMaxVersion();

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("update RecommendationModel m set m.active = false where m.active = true")
    int deactivateAll();
}
