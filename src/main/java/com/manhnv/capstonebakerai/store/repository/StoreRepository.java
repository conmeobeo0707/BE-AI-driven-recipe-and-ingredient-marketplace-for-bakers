package com.manhnv.capstonebakerai.store.repository;

import com.manhnv.capstonebakerai.store.entity.Store;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.UUID;

public interface StoreRepository extends JpaRepository<Store, UUID> {
}
