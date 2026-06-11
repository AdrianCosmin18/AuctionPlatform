package org.nedelcu.cosmin.auction.api.auth;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.nedelcu.cosmin.auction.api.auth.dto.AuthResponse;
import org.nedelcu.cosmin.auction.api.auth.dto.AuthenticatedUserResponse;
import org.nedelcu.cosmin.auction.api.auth.dto.LoginRequest;
import org.nedelcu.cosmin.auction.api.auth.dto.RegisterRequest;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {
    private final AuthService authService;

    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    public AuthResponse register(@Valid @RequestBody RegisterRequest request) {
        return authService.register(request);
    }

    @PostMapping("/login")
    public AuthResponse login(@Valid @RequestBody LoginRequest request) {
        return authService.login(request);
    }

    @GetMapping("/me")
    public AuthenticatedUserResponse me() {
        return authService.me();
    }
}
