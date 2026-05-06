package com.jobson.market.auth.infrastructure.web;

import com.jobson.market.auth.application.usecase.LoginWithPasswordCommand;
import com.jobson.market.auth.application.usecase.LoginWithPasswordResult;
import com.jobson.market.auth.application.usecase.LoginWithPasswordUseCase;
import com.jobson.market.auth.application.usecase.LogoutCommand;
import com.jobson.market.auth.application.usecase.LogoutUseCase;
import com.jobson.market.auth.application.usecase.RefreshTokenCommand;
import com.jobson.market.auth.application.usecase.RefreshTokenUseCase;
import com.jobson.market.auth.application.usecase.RegisterUserCommand;
import com.jobson.market.auth.application.usecase.RegisterUserResult;
import com.jobson.market.auth.application.usecase.RegisterUserUseCase;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import java.util.Map;
import java.util.UUID;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/auth")
class AuthController {

  private final RegisterUserUseCase registerUser;
  private final LoginWithPasswordUseCase loginWithPassword;
  private final RefreshTokenUseCase refreshToken;
  private final LogoutUseCase logout;

  AuthController(
      RegisterUserUseCase registerUser,
      LoginWithPasswordUseCase loginWithPassword,
      RefreshTokenUseCase refreshToken,
      LogoutUseCase logout) {
    this.registerUser = registerUser;
    this.loginWithPassword = loginWithPassword;
    this.refreshToken = refreshToken;
    this.logout = logout;
  }

  @PostMapping("/register")
  ResponseEntity<RegisterUserResult> register(@Valid @RequestBody RegisterRequest request) {
    return ResponseEntity.status(HttpStatus.CREATED)
        .body(registerUser.register(new RegisterUserCommand(request.email(), request.password())));
  }

  @PostMapping("/login")
  ResponseEntity<LoginWithPasswordResult> login(@Valid @RequestBody LoginRequest request) {
    LoginWithPasswordResult result =
        loginWithPassword.login(new LoginWithPasswordCommand(request.email(), request.password()));
    return withRefreshCookie(result);
  }

  @PostMapping("/refresh")
  ResponseEntity<LoginWithPasswordResult> refresh(@Valid @RequestBody RefreshRequest request) {
    LoginWithPasswordResult result =
        refreshToken.refresh(new RefreshTokenCommand(request.refreshToken()));
    return withRefreshCookie(result);
  }

  @PostMapping("/logout")
  ResponseEntity<Void> logout(@Valid @RequestBody RefreshRequest request) {
    logout.logout(new LogoutCommand(request.refreshToken()));
    return ResponseEntity.noContent().build();
  }

  @PostMapping("/logout-all")
  ResponseEntity<Void> logoutAll(@AuthenticationPrincipal Jwt jwt) {
    logout.logoutAll(UUID.fromString(jwt.getSubject()));
    return ResponseEntity.noContent().build();
  }

  @GetMapping("/me")
  Map<String, Object> me(@AuthenticationPrincipal Jwt jwt) {
    return Map.of(
        "userId", jwt.getSubject(),
        "email", jwt.getClaimAsString("email"),
        "emailVerified", jwt.getClaim("email_verified"));
  }

  record RegisterRequest(@NotBlank String email, @NotBlank String password) {}

  record LoginRequest(@NotBlank String email, @NotBlank String password) {}

  record RefreshRequest(@NotBlank String refreshToken) {}

  private ResponseEntity<LoginWithPasswordResult> withRefreshCookie(
      LoginWithPasswordResult result) {
    ResponseCookie cookie =
        ResponseCookie.from("refresh_token", result.refreshToken())
            .httpOnly(true)
            .secure(true)
            .sameSite("Lax")
            .path("/auth")
            .build();
    return ResponseEntity.ok().header(HttpHeaders.SET_COOKIE, cookie.toString()).body(result);
  }
}
