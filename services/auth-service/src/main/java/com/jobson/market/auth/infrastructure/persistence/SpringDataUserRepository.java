package com.jobson.market.auth.infrastructure.persistence;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

interface SpringDataUserRepository extends JpaRepository<UserEntity, UUID> {

  boolean existsByEmail(String email);

  Optional<UserEntity> findByEmail(String email);
}
