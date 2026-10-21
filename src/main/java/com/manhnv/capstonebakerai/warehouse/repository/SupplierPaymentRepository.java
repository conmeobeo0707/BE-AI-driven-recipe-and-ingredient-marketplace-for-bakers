package com.manhnv.capstonebakerai.warehouse.repository;

import com.manhnv.capstonebakerai.warehouse.entity.SupplierPayment;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface SupplierPaymentRepository extends JpaRepository<SupplierPayment, UUID> {
    boolean existsByCode(String code);

    Page<SupplierPayment> findBySupplierId(UUID supplierId, Pageable pageable);

    Optional<SupplierPayment> findByIdAndSupplierId(UUID id, UUID supplierId);
}
