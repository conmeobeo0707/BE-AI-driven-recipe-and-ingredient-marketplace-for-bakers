package com.manhnv.capstonebakerai.hobby.repository;

import com.manhnv.capstonebakerai.hobby.entity.Hobby;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Set;
import java.util.UUID;

public interface HobbyRepository extends JpaRepository<Hobby, UUID> {

    List<Hobby> findAllByOrderByNameAsc();

    List<Hobby> findAllByActiveTrueOrderByNameAsc();

    List<Hobby> findAllByIdIn(Set<UUID> ids);

    List<Hobby> findAllByIdInAndActiveTrue(Set<UUID> ids);

    boolean existsByNameIgnoreCase(String name);

    boolean existsByNameIgnoreCaseAndIdNot(String name, UUID id);
}