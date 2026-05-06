package unit.domain;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.jobson.market.auth.domain.model.Password;
import org.junit.jupiter.api.Test;

class PasswordTest {

  @Test
  void shouldAcceptStrongPassword() {
    Password password = assertDoesNotThrow(() -> new Password("Str0ng-password!"));

    assertEquals("Str0ng-password!", password.value());
  }

  @Test
  void shouldRejectBlankPassword() {
    IllegalArgumentException exception =
        assertThrows(IllegalArgumentException.class, () -> new Password(" "));

    assertEquals("Password is required", exception.getMessage());
  }

  @Test
  void shouldRejectWeakPassword() {
    IllegalArgumentException exception =
        assertThrows(IllegalArgumentException.class, () -> new Password("password"));

    assertEquals("Password does not meet security policy", exception.getMessage());
  }
}
