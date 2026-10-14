package edu.platform.education.users;

import edu.platform.education.users.security.JwtService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
class AuthService {

    private final AuthenticationManager authenticationManager;
    private final UserDetailsService userDetailsService;
    private final JwtService jwtService;

    @Transactional(readOnly = true)
    public String authenticate(LoginRequest request) {
        // 1. Проверяем креды (бросит BadCredentialsException, если неверно)
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.email(), request.password())
        );

        // 2. Грузим UserDetails и генерируем токен
        UserDetails userDetails = userDetailsService.loadUserByUsername(request.email());
        return jwtService.generateToken(userDetails);
    }
}