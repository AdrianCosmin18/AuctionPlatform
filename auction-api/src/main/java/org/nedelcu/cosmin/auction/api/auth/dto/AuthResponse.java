package org.nedelcu.cosmin.auction.api.auth.dto;

public record AuthResponse(
        String accessToken,
        String tokenType,
        long expiresInMs,
        AuthenticatedUserResponse user
) {
}
