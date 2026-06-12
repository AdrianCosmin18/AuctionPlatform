package org.nedelcu.cosmin.auction.api.user.profile.dto;

public record UserProfileResponse(
        String email,
        String firstName,
        String lastName,
        String phone,
        String country,
        String city,
        String addressLine1,
        String addressLine2,
        String postalCode
) {
}
