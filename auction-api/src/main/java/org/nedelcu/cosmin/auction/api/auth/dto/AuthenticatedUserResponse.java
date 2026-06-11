package org.nedelcu.cosmin.auction.api.auth.dto;

import org.nedelcu.cosmin.auction.api.user.UserRole;

public record AuthenticatedUserResponse(
        Long id,
        String email,
        UserRole role
) {
}
