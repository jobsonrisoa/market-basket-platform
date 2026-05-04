package unit.domain;

import static org.junit.jupiter.api.Assertions.*;

import com.jobson.market.auth.domain.model.Email;
import org.junit.jupiter.api.Test;

class EmailTest {

  @Test
  void shouldCreateValidEmail() {
    Email email = new Email("john@example.com");

    assertEquals("john@example.com", email.value());
  }

  @Test
  void shouldRejectInvalidEmail() {
    IllegalArgumentException exception =
        assertThrows(IllegalArgumentException.class, () -> new Email("invalid-email"));

    assertEquals("Invalid email", exception.getMessage());
  }
}
