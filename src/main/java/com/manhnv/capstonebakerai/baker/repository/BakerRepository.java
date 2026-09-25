package com.manhnv.capstonebakerai.baker.repository;

import com.manhnv.capstonebakerai.baker.entity.Baker;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface BakerRepository extends JpaRepository<Baker, UUID> {
}
