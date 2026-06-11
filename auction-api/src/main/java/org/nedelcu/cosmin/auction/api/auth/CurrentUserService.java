package org.nedelcu.cosmin.auction.api.auth;

import org.nedelcu.cosmin.auction.api.common.exception.BusinessException;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

@Service
public class CurrentUserService {

    public Long getCurrentUserId() {
        return principal().getId();
    }

    public CurrentUserPrincipal getCurrentUser() {
        return principal();
    }

    private CurrentUserPrincipal principal() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null
                || !authentication.isAuthenticated()
                || authentication instanceof AnonymousAuthenticationToken
                || !(authentication.getPrincipal() instanceof CurrentUserPrincipal principal)) {
            throw new BusinessException("Authenticated user is required");
        }
        return principal;
    }
}
