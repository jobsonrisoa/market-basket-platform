package com.jobson.market.auth.infrastructure.web;

import com.jobson.market.auth.application.usecase.admin.AdminUserManagementUseCase;
import com.jobson.market.auth.application.usecase.admin.AssignRoleCommand;
import com.jobson.market.auth.application.usecase.admin.RemoveRoleCommand;
import com.jobson.market.auth.application.usecase.authentication.LoginWithPasswordCommand;
import com.jobson.market.auth.application.usecase.authentication.LoginWithPasswordResult;
import com.jobson.market.auth.application.usecase.authentication.LoginWithPasswordUseCase;
import com.jobson.market.auth.application.usecase.authentication.LogoutCommand;
import com.jobson.market.auth.application.usecase.authentication.LogoutUseCase;
import com.jobson.market.auth.application.usecase.authentication.RefreshTokenCommand;
import com.jobson.market.auth.application.usecase.authentication.RefreshTokenUseCase;
import com.jobson.market.auth.application.usecase.registration.RegisterUserCommand;
import com.jobson.market.auth.application.usecase.registration.RegisterUserResult;
import com.jobson.market.auth.application.usecase.registration.RegisterUserUseCase;
import com.jobson.market.auth.domain.model.Role;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import java.util.Map;
import java.util.UUID;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
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
  private final AdminUserManagementUseCase adminUserManagement;

  AuthController(
      RegisterUserUseCase registerUser,
      LoginWithPasswordUseCase loginWithPassword,
      RefreshTokenUseCase refreshToken,
      LogoutUseCase logout,
      AdminUserManagementUseCase adminUserManagement) {
    this.registerUser = registerUser;
    this.loginWithPassword = loginWithPassword;
    this.refreshToken = refreshToken;
    this.logout = logout;
    this.adminUserManagement = adminUserManagement;
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
        "emailVerified", jwt.getClaim("email_verified"),
        "roles", jwt.getClaimAsStringList("roles"),
        "accountProfile", jwt.getClaimAsString("account_profile"),
        "customerProfileType", jwt.getClaimAsString("customer_profile_type"));
  }

  @PostMapping("/admin/users/{userId}/roles")
  @PreAuthorize("hasRole('ADMIN')")
  ResponseEntity<Void> assignRole(
      @AuthenticationPrincipal Jwt jwt,
      @PathVariable UUID userId,
      @Valid @RequestBody RoleRequest request) {
    adminUserManagement.assignRole(
        new AssignRoleCommand(UUID.fromString(jwt.getSubject()), userId, request.role()));
    return ResponseEntity.noContent().build();
  }

  @DeleteMapping("/admin/users/{userId}/roles/{role}")
  @PreAuthorize("hasRole('ADMIN')")
  ResponseEntity<Void> removeRole(
      @AuthenticationPrincipal Jwt jwt, @PathVariable UUID userId, @PathVariable Role role) {
    adminUserManagement.removeRole(
        new RemoveRoleCommand(UUID.fromString(jwt.getSubject()), userId, role));
    return ResponseEntity.noContent().build();
  }

  @PostMapping("/admin/users/{userId}/suspend")
  @PreAuthorize("hasRole('ADMIN')")
  ResponseEntity<Void> suspend(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID userId) {
    adminUserManagement.suspend(UUID.fromString(jwt.getSubject()), userId);
    return ResponseEntity.noContent().build();
  }

  @PostMapping("/admin/users/{userId}/reactivate")
  @PreAuthorize("hasRole('ADMIN')")
  ResponseEntity<Void> reactivate(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID userId) {
    adminUserManagement.reactivate(UUID.fromString(jwt.getSubject()), userId);
    return ResponseEntity.noContent().build();
  }

  record RegisterRequest(@NotBlank String email, @NotBlank String password) {}

  record LoginRequest(@NotBlank String email, @NotBlank String password) {}

  record RefreshRequest(@NotBlank String refreshToken) {}

  record RoleRequest(Role role) {}

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
