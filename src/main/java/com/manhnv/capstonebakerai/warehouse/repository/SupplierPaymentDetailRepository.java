package com.manhnv.capstonebakerai.warehouse.repository;

import com.manhnv.capstonebakerai.warehouse.entity.SupplierPaymentDetail;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface SupplierPaymentDetailRepository extends JpaRepository<SupplierPaymentDetail, UUID> {
    List<SupplierPaymentDetail> findBySupplierPaymentsIdOrderById(UUID supplierPaymentsId);
}
