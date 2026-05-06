package com.jobson.market.auth.application.port;

public interface RefreshTokenCodec {

  String generateRawToken();

  String hash(String rawToken);
}
