package edu.platform.education.users.security;

import edu.platform.education.users.User;
import edu.platform.education.users.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class UserDetailsServiceImpl implements UserDetailsService {

    private final UserService userService;

    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        try {
            User user = userService.findByEmail(email);
            return new UserPrincipal(user);
        } catch (RuntimeException e) {
            // UserService бросает ResourceNotFoundException — конвертируем в UsernameNotFoundException,
            // как того ожидает Spring Security
            throw new UsernameNotFoundException("Пользователь с email " + email + " не найден");
        }
    }
}