package org.nedelcu.cosmin.auction.api.user.profile.dto;

import jakarta.validation.constraints.Size;

public record UpdateUserProfileRequest(
        @Size(max = 100)
        String firstName,

        @Size(max = 100)
        String lastName,

        @Size(max = 50)
        String phone,

        @Size(max = 100)
        String country,

        @Size(max = 100)
        String city,

        @Size(max = 255)
        String addressLine1,

        @Size(max = 255)
        String addressLine2,

        @Size(max = 50)
        String postalCode
) {
}
