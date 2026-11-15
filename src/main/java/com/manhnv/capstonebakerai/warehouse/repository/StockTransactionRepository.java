package com.manhnv.capstonebakerai.warehouse.repository;

import com.manhnv.capstonebakerai.warehouse.entity.StockTransaction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.UUID;

public interface StockTransactionRepository extends JpaRepository<StockTransaction, UUID>,
        JpaSpecificationExecutor<StockTransaction> {
}
