package com.manhnv.capstonebakerai.warehouse.repository;
import com.manhnv.capstonebakerai.warehouse.entity.Supplier;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.UUID;
public interface SupplierRepository extends JpaRepository<Supplier, UUID> {
    @Query("select s from Supplier s where lower(s.supplierName) like lower(concat('%', :keyword, '%'))")
    Page<Supplier> search(@Param("keyword") String keyword, Pageable pageable);
}
