package com.manhnv.capstonebakerai.shipment.repository;

import com.manhnv.capstonebakerai.shipment.entity.Shipment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface ShipmentRepository extends JpaRepository<Shipment, UUID> {
    Optional<Shipment> findByOrderId(UUID orderId);
    Optional<Shipment> findByExternalId(String externalId);
    boolean existsByOrderId(UUID orderId);
}
