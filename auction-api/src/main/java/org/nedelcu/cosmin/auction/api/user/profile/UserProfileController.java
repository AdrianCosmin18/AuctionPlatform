package org.nedelcu.cosmin.auction.api.user.profile;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.nedelcu.cosmin.auction.api.auth.CurrentUserService;
import org.nedelcu.cosmin.auction.api.user.profile.dto.ChangePasswordRequest;
import org.nedelcu.cosmin.auction.api.user.profile.dto.UpdateUserProfileRequest;
import org.nedelcu.cosmin.auction.api.user.profile.dto.UserProfileResponse;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/me/profile")
@RequiredArgsConstructor
public class UserProfileController {

    private final UserProfileService userProfileService;
    private final CurrentUserService currentUserService;

    @GetMapping
    public UserProfileResponse getProfile() {
        return userProfileService.getProfile(currentUserService.getCurrentUserId());
    }

    @PutMapping
    public UserProfileResponse updateProfile(@Valid @RequestBody UpdateUserProfileRequest request) {
        return userProfileService.updateProfile(currentUserService.getCurrentUserId(), request);
    }

    @PutMapping("/password")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void changePassword(@Valid @RequestBody ChangePasswordRequest request) {
        userProfileService.changePassword(currentUserService.getCurrentUserId(), request);
    }
}
