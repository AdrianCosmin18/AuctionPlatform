package org.nedelcu.cosmin.auction.api.auth;

import lombok.RequiredArgsConstructor;
import org.nedelcu.cosmin.auction.api.common.exception.ResourceNotFoundException;
import org.nedelcu.cosmin.auction.api.user.UserRepository;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class CustomUserDetailsService implements UserDetailsService {
    private final UserRepository userRepository;

    @Override
    public UserDetails loadUserByUsername(String username) {
        return userRepository.findByEmailIgnoreCase(username)
                .map(CurrentUserPrincipal::new)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + username));
    }
}
