package com.manhnv.capstonebakerai.identity.repository;

import com.manhnv.capstonebakerai.identity.entity.User;
import com.manhnv.capstonebakerai.identity.enums.Role;
import org.springframework.data.jpa.repository.JpaRepository;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.UUID;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, UUID> {
    boolean existsByEmailIgnoreCase(String email);
    Optional<User> findByEmailIgnoreCase(String email);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select u from User u where lower(u.email) = lower(:email)")
    Optional<User> findByEmailIgnoreCaseForUpdate(@Param("email") String email);

    List<User> findByRoleInOrderByCreatedAtDescUserIdAsc(
            Collection<Role> roles
    );
}
