package com.manhnv.capstonebakerai.learner.repository;

import com.manhnv.capstonebakerai.learner.entity.Learner;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.UUID;

public interface LearnerRepository extends JpaRepository<Learner, UUID> {
}
