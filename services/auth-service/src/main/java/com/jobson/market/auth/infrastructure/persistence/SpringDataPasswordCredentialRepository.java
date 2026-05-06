package com.jobson.market.auth.infrastructure.persistence;

import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

interface SpringDataPasswordCredentialRepository
    extends JpaRepository<PasswordCredentialEntity, UUID> {}
