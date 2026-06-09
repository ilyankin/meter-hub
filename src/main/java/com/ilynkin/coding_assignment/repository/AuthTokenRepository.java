package com.ilynkin.coding_assignment.repository;

import com.ilynkin.coding_assignment.entity.AuthToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

public interface AuthTokenRepository extends JpaRepository<AuthToken, UUID> {

    @Query("select t from AuthToken t join fetch t.user where t.token = :token")
    Optional<AuthToken> findByToken(@Param("token") String token);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("delete from AuthToken t where t.expiresAt < :now")
    int deleteAllByExpiresAtBefore(@Param("now") Instant now);
}
