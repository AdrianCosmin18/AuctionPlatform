package org.nedelcu.cosmin.auction.api.user.profile;

import java.time.OffsetDateTime;
import lombok.RequiredArgsConstructor;
import org.nedelcu.cosmin.auction.api.common.exception.BusinessException;
import org.nedelcu.cosmin.auction.api.user.UserEntity;
import org.nedelcu.cosmin.auction.api.user.UserRepository;
import org.nedelcu.cosmin.auction.api.user.profile.dto.ChangePasswordRequest;
import org.nedelcu.cosmin.auction.api.user.profile.dto.UpdateUserProfileRequest;
import org.nedelcu.cosmin.auction.api.user.profile.dto.UserProfileResponse;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class UserProfileService {

    private final UserRepository userRepository;
    private final UserProfileRepository userProfileRepository;
    private final PasswordEncoder passwordEncoder;

    @Transactional(readOnly = true)
    public UserProfileResponse getProfile(Long userId) {
        UserEntity user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException("User not found"));
        UserProfileEntity profile = userProfileRepository.findByUserId(userId).orElse(null);
        return toResponse(user.getEmail(), profile);
    }

    @Transactional
    public UserProfileResponse updateProfile(Long userId, UpdateUserProfileRequest request) {
        UserEntity user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException("User not found"));

        UserProfileEntity profile = userProfileRepository.findByUserId(userId)
                .orElseGet(() -> {
                    UserProfileEntity entity = new UserProfileEntity();
                    entity.setUserId(userId);
                    return entity;
                });

        profile.setFirstName(normalize(request.firstName()));
        profile.setLastName(normalize(request.lastName()));
        profile.setPhone(normalize(request.phone()));
        profile.setCountry(normalize(request.country()));
        profile.setCity(normalize(request.city()));
        profile.setAddressLine1(normalize(request.addressLine1()));
        profile.setAddressLine2(normalize(request.addressLine2()));
        profile.setPostalCode(normalize(request.postalCode()));
        profile.setUpdatedAt(OffsetDateTime.now());

        UserProfileEntity savedProfile = userProfileRepository.save(profile);
        return toResponse(user.getEmail(), savedProfile);
    }

    @Transactional
    public void changePassword(Long userId, ChangePasswordRequest request) {
        UserEntity user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException("User not found"));

        if (!passwordEncoder.matches(request.currentPassword(), user.getPasswordHash())) {
            throw new BusinessException("Current password is incorrect");
        }

        if (!request.newPassword().equals(request.confirmPassword())) {
            throw new BusinessException("New password and confirmation must match");
        }

        if (request.currentPassword().equals(request.newPassword())) {
            throw new BusinessException("New password must be different from the current password");
        }

        user.setPasswordHash(passwordEncoder.encode(request.newPassword()));
        userRepository.save(user);
    }

    @Transactional(readOnly = true)
    public UserProfileSummary getProfileSummary(Long userId) {
        return userProfileRepository.findByUserId(userId)
                .map(profile -> new UserProfileSummary(profile.getFirstName(), profile.getLastName()))
                .orElse(new UserProfileSummary(null, null));
    }

    private UserProfileResponse toResponse(String email, UserProfileEntity profile) {
        return new UserProfileResponse(
                email,
                profile != null ? profile.getFirstName() : null,
                profile != null ? profile.getLastName() : null,
                profile != null ? profile.getPhone() : null,
                profile != null ? profile.getCountry() : null,
                profile != null ? profile.getCity() : null,
                profile != null ? profile.getAddressLine1() : null,
                profile != null ? profile.getAddressLine2() : null,
                profile != null ? profile.getPostalCode() : null
        );
    }

    private String normalize(String value) {
        if (value == null) {
            return null;
        }

        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    public record UserProfileSummary(
            String firstName,
            String lastName
    ) {
    }
}
