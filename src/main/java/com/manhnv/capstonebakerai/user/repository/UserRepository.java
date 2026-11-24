package com.manhnv.capstonebakerai.user.repository;

import com.manhnv.capstonebakerai.user.dto.UserSummaryResponse;
import com.manhnv.capstonebakerai.user.entity.User;
import com.manhnv.capstonebakerai.user.enums.Role;
import com.manhnv.capstonebakerai.user.enums.UserStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.UUID;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, UUID>, JpaSpecificationExecutor<User> {
    boolean existsByEmailIgnoreCase(String email);
    Optional<User> findByEmailIgnoreCase(String email);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select u from User u where lower(u.email) = lower(:email)")
    Optional<User> findByEmailIgnoreCaseForUpdate(@Param("email") String email);

    List<User> findByRoleInOrderByCreatedAtDescUserIdAsc(
            Collection<Role> roles
    );

    List<User> findByRoleInAndStatusOrderByCreatedAtDescUserIdAsc(
            Collection<Role> roles,
            UserStatus status
    );

    @Query("select new com.manhnv.capstonebakerai.user.dto.UserSummaryResponse(u.userId, u.fullName, u.avatarUrl) " +
            "from User u where u.userId in :ids")
    List<UserSummaryResponse> findSummariesByIdIn(@Param("ids") Collection<UUID> ids);
}
