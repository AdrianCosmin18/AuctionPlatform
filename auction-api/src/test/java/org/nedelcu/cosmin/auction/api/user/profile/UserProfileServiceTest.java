package org.nedelcu.cosmin.auction.api.user.profile;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.nedelcu.cosmin.auction.api.common.exception.BusinessException;
import org.nedelcu.cosmin.auction.api.user.UserEntity;
import org.nedelcu.cosmin.auction.api.user.UserRepository;
import org.nedelcu.cosmin.auction.api.user.profile.dto.ChangePasswordRequest;
import org.nedelcu.cosmin.auction.api.user.profile.dto.UpdateUserProfileRequest;
import org.nedelcu.cosmin.auction.api.user.profile.dto.UserProfileResponse;
import org.springframework.security.crypto.password.PasswordEncoder;

@ExtendWith(MockitoExtension.class)
class UserProfileServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private UserProfileRepository userProfileRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private UserProfileService userProfileService;

    @Test
    void updateProfileCreatesOrUpdatesProfileData() {
        UserEntity user = user(7L, "collector@example.com", "encoded-password");
        when(userRepository.findById(7L)).thenReturn(Optional.of(user));
        when(userProfileRepository.findByUserId(7L)).thenReturn(Optional.empty());
        when(userProfileRepository.save(any(UserProfileEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));

        UserProfileResponse response = userProfileService.updateProfile(7L, new UpdateUserProfileRequest(
                "  Cosmin ",
                " Nadelcu ",
                " 0712345678 ",
                "Romania",
                "Bucharest",
                "Str. Academiei 1",
                "",
                "010101"
        ));

        assertThat(response.email()).isEqualTo("collector@example.com");
        assertThat(response.firstName()).isEqualTo("Cosmin");
        assertThat(response.lastName()).isEqualTo("Nadelcu");
        assertThat(response.addressLine2()).isNull();
    }

    @Test
    void changePasswordRejectsIncorrectCurrentPassword() {
        UserEntity user = user(7L, "collector@example.com", "encoded-password");
        when(userRepository.findById(7L)).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("wrong-password", "encoded-password")).thenReturn(false);

        assertThatThrownBy(() -> userProfileService.changePassword(7L, new ChangePasswordRequest(
                "wrong-password",
                "new-password-123",
                "new-password-123"
        )))
                .isInstanceOf(BusinessException.class)
                .hasMessage("Current password is incorrect");

        verify(userRepository, never()).save(any(UserEntity.class));
    }

    @Test
    void changePasswordEncodesAndSavesNewPassword() {
        UserEntity user = user(7L, "collector@example.com", "encoded-password");
        when(userRepository.findById(7L)).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("current-password", "encoded-password")).thenReturn(true);
        when(passwordEncoder.encode("new-password-123")).thenReturn("new-encoded-password");

        userProfileService.changePassword(7L, new ChangePasswordRequest(
                "current-password",
                "new-password-123",
                "new-password-123"
        ));

        assertThat(user.getPasswordHash()).isEqualTo("new-encoded-password");
        verify(userRepository).save(user);
    }

    private UserEntity user(Long id, String email, String passwordHash) {
        UserEntity user = new UserEntity();
        user.setId(id);
        user.setEmail(email);
        user.setPasswordHash(passwordHash);
        return user;
    }
}
