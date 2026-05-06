package com.jobson.market.auth.application.port.crypto;

public interface RefreshTokenCodec {

  String generateRawToken();

  String hash(String rawToken);
}
