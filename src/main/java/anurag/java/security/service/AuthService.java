package anurag.java.security.service;

import java.nio.charset.StandardCharsets;
import java.util.Collection;
import java.util.List;

import anurag.java.security.entity.AppUser;
import anurag.java.security.entity.AppUserAuthority;
import anurag.java.security.entity.AppUserAuthorityId;
import anurag.java.security.repository.AppUserAuthorityRepository;
import anurag.java.security.repository.AppUserRepository;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class AuthService {

    private static final String ROLE_USER = "ROLE_USER";
    private static final int MAX_PASSWORD_BYTES = 72;

    private final AuthenticationManager authenticationManager;
    private final AppUserRepository appUserRepository;
    private final AppUserAuthorityRepository appUserAuthorityRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    public AuthService(
            AuthenticationManager authenticationManager,
            AppUserRepository appUserRepository,
            AppUserAuthorityRepository appUserAuthorityRepository,
            PasswordEncoder passwordEncoder,
            JwtService jwtService) {
        this.authenticationManager = authenticationManager;
        this.appUserRepository = appUserRepository;
        this.appUserAuthorityRepository = appUserAuthorityRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
    }

    @Transactional
    public AuthResponse register(AuthRequest request) {
        validateCredentials(request);
        if (request.password().length() < 8) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Password must be at least 8 characters");
        }
        if (appUserRepository.existsById(request.username())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Username is already registered");
        }

        AppUser user = appUserRepository.save(
            new AppUser(request.username(), passwordEncoder.encode(request.password()), true));
        appUserAuthorityRepository.save(new AppUserAuthority(
            new AppUserAuthorityId(user.getUsername(), ROLE_USER), user));

        return tokenFor(user.getUsername(), List.of(new SimpleGrantedAuthority(ROLE_USER)));
    }

    public AuthResponse login(AuthRequest request) {
        validateCredentials(request);
        Authentication authentication = authenticationManager.authenticate(
            UsernamePasswordAuthenticationToken.unauthenticated(
                request.username(), request.password()));
        UserDetails user = (UserDetails) authentication.getPrincipal();
        return tokenFor(user.getUsername(), user.getAuthorities());
    }

    private AuthResponse tokenFor(String username, Collection<? extends GrantedAuthority> authorities) {
        return new AuthResponse(
            jwtService.createToken(username, authorities),
            "Bearer",
            jwtService.expirationSeconds());
    }

    private void validateCredentials(AuthRequest request) {
        if (request == null || request.username() == null || request.username().isBlank()
                || request.username().length() > 100 || request.password() == null
                || request.password().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Username and password are required");
        }
        if (request.password().getBytes(StandardCharsets.UTF_8).length > MAX_PASSWORD_BYTES) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Password exceeds the supported length");
        }
    }
}
