package org.nedelcu.cosmin.auction.api.auth;

import java.time.OffsetDateTime;
import lombok.RequiredArgsConstructor;
import org.nedelcu.cosmin.auction.api.auth.dto.AuthResponse;
import org.nedelcu.cosmin.auction.api.auth.dto.AuthenticatedUserResponse;
import org.nedelcu.cosmin.auction.api.auth.dto.LoginRequest;
import org.nedelcu.cosmin.auction.api.auth.dto.RegisterRequest;
import org.nedelcu.cosmin.auction.api.common.exception.BusinessException;
import org.nedelcu.cosmin.auction.api.user.UserEntity;
import org.nedelcu.cosmin.auction.api.user.UserRepository;
import org.nedelcu.cosmin.auction.api.user.UserRole;
import org.nedelcu.cosmin.auction.api.user.profile.UserProfileService;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AuthService {
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;
    private final CurrentUserService currentUserService;
    private final UserProfileService userProfileService;

    @Transactional
    public AuthResponse register(RegisterRequest request) {
        String normalizedEmail = request.email().trim().toLowerCase();
        if (userRepository.existsByEmailIgnoreCase(normalizedEmail)) {
            throw new BusinessException("An account with this email already exists");
        }

        UserEntity user = new UserEntity();
        user.setEmail(normalizedEmail);
        user.setPasswordHash(passwordEncoder.encode(request.password()));
        user.setRole(UserRole.USER);
        user.setCreatedAt(OffsetDateTime.now());

        UserEntity savedUser = userRepository.save(user);
        CurrentUserPrincipal principal = new CurrentUserPrincipal(savedUser);
        return toAuthResponse(principal);
    }

    public AuthResponse login(LoginRequest request) {
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.email().trim().toLowerCase(), request.password())
        );

        CurrentUserPrincipal principal = (CurrentUserPrincipal) userRepository.findByEmailIgnoreCase(request.email().trim().toLowerCase())
                .map(CurrentUserPrincipal::new)
                .orElseThrow(() -> new BusinessException("Invalid email or password"));
        return toAuthResponse(principal);
    }

    public AuthenticatedUserResponse me() {
        CurrentUserPrincipal principal = currentUserService.getCurrentUser();
        return authenticatedUserResponse(principal);
    }

    private AuthResponse toAuthResponse(CurrentUserPrincipal principal) {
        return new AuthResponse(
                jwtService.generateToken(principal),
                "Bearer",
                jwtService.getExpirationMs(),
                authenticatedUserResponse(principal)
        );
    }

    private AuthenticatedUserResponse authenticatedUserResponse(CurrentUserPrincipal principal) {
        UserProfileService.UserProfileSummary summary = userProfileService.getProfileSummary(principal.getId());
        return new AuthenticatedUserResponse(
                principal.getId(),
                principal.getEmail(),
                UserRole.valueOf(principal.getRole()),
                summary.firstName(),
                summary.lastName()
        );
    }
}
