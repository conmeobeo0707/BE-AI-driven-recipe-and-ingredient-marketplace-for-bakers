package com.manhnv.capstonebakerai.receipt.repository;

import com.manhnv.capstonebakerai.receipt.entity.ReceiptCategory;
import com.manhnv.capstonebakerai.receipt.enums.ReceiptCategoryType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface ReceiptCategoryRepository extends JpaRepository<ReceiptCategory, UUID> {
    boolean existsByNameIgnoreCase(String name);
    boolean existsByNameIgnoreCaseAndIdNot(String name, UUID id);
    Optional<ReceiptCategory> findFirstByTypeOrderByCreatedAtAsc(ReceiptCategoryType type);
    boolean existsByType(ReceiptCategoryType type);
}
