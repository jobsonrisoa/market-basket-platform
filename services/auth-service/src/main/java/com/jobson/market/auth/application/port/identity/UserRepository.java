package com.jobson.market.auth.application.port.identity;

import com.jobson.market.auth.domain.model.Email;
import com.jobson.market.auth.domain.model.User;
import java.util.Optional;
import java.util.UUID;

public interface UserRepository {

  boolean existsByEmail(Email email);

  Optional<User> findByEmail(Email email);

  Optional<User> findById(UUID id);

  User save(User user);
}
