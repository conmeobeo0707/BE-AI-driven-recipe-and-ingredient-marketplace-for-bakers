package com.manhnv.capstonebakerai.warehouse.repository;
import com.manhnv.capstonebakerai.warehouse.entity.Supplier;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.UUID;
public interface SupplierRepository extends JpaRepository<Supplier, UUID> { }
