package com.jobson.market.auth.infrastructure.security;

import com.jobson.market.auth.application.usecase.authentication.GoogleLoginCommand;
import com.jobson.market.auth.application.usecase.authentication.GoogleLoginUseCase;
import com.jobson.market.auth.application.usecase.authentication.LoginWithPasswordResult;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseCookie;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

@Component
class GoogleOAuth2SuccessHandler implements AuthenticationSuccessHandler {

  private final GoogleLoginUseCase googleLogin;

  GoogleOAuth2SuccessHandler(GoogleLoginUseCase googleLogin) {
    this.googleLogin = googleLogin;
  }

  @Override
  public void onAuthenticationSuccess(
      HttpServletRequest request, HttpServletResponse response, Authentication authentication)
      throws IOException {
    if (!(authentication.getPrincipal() instanceof OidcUser oidcUser)) {
      response.sendError(HttpServletResponse.SC_UNAUTHORIZED);
      return;
    }
    LoginWithPasswordResult result =
        googleLogin.login(
            new GoogleLoginCommand(
                oidcUser.getSubject(), oidcUser.getEmail(), oidcUser.getEmailVerified()));
    response.setContentType(MediaType.APPLICATION_JSON_VALUE);
    ResponseCookie cookie =
        ResponseCookie.from("refresh_token", result.refreshToken())
            .httpOnly(true)
            .secure(true)
            .sameSite("Lax")
            .path("/auth")
            .build();
    response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());
    response
        .getWriter()
        .write(
            "{\"accessToken\":\"%s\",\"refreshToken\":\"%s\"}"
                .formatted(result.accessToken(), result.refreshToken()));
  }
}
