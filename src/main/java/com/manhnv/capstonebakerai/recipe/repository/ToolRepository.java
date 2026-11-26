package com.manhnv.capstonebakerai.recipe.repository;

import com.manhnv.capstonebakerai.recipe.entity.Tool;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

public interface ToolRepository extends JpaRepository<Tool, UUID> {

    Page<Tool> findByNameContainingIgnoreCase(String name, Pageable pageable);

    List<Tool> findAllByOrderByNameAscIdAsc();

    @Query(value = "SELECT EXISTS (SELECT 1 FROM tools WHERE lower(btrim(name)) = lower(btrim(:name)))",
            nativeQuery = true)
    boolean existsByNormalizedName(@Param("name") String name);

    @Query(value = "SELECT EXISTS (SELECT 1 FROM tools WHERE lower(btrim(name)) = lower(btrim(:name)) AND id <> :id)",
            nativeQuery = true)
    boolean existsByNormalizedNameAndIdNot(@Param("name") String name, @Param("id") UUID id);
}
