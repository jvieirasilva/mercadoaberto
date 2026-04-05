package com.security.service;

import com.security.model.Role;
import com.security.model.User;
import com.security.repository.UserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("CustomUserDetailsService Tests")
class CustomUserDetailsServiceTest {

    @Mock private UserRepository userRepository;
    @InjectMocks private CustomUserDetailsService service;

    private User buildUser(String email) {
        User user = new User();
        user.setEmail(email);
        user.setPassword("encodedPassword");
        user.setRole(Role.USER);
        user.setActive(true);
        user.setFullName("Jose Silva");
        return user;
    }

    @Nested
    @DisplayName("loadUserByUsername")
    class LoadUserTests {

        @Test
        @DisplayName("deve carregar utilizador existente")
        void shouldLoadExistingUser() {
            User user = buildUser("jose@test.com");
            when(userRepository.findByEmail("jose@test.com")).thenReturn(Optional.of(user));

            UserDetails details = service.loadUserByUsername("jose@test.com");

            assertThat(details).isNotNull();
            assertThat(details.getUsername()).isEqualTo("jose@test.com");
            assertThat(details.getPassword()).isEqualTo("encodedPassword");
            assertThat(details.getAuthorities()).anyMatch(a -> a.getAuthority().equals("ROLE_USER"));
        }

        @Test
        @DisplayName("deve lançar UsernameNotFoundException para utilizador inexistente")
        void shouldThrowWhenUserNotFound() {
            when(userRepository.findByEmail("naoexiste@test.com")).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.loadUserByUsername("naoexiste@test.com"))
                    .isInstanceOf(UsernameNotFoundException.class)
                    .hasMessageContaining("naoexiste@test.com");
        }

        @Test
        @DisplayName("deve carregar utilizador com role ADMIN")
        void shouldLoadAdminUser() {
            User user = buildUser("admin@test.com");
            user.setRole(Role.ADMIN);
            when(userRepository.findByEmail("admin@test.com")).thenReturn(Optional.of(user));

            UserDetails details = service.loadUserByUsername("admin@test.com");

            assertThat(details.getAuthorities()).anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));
        }
    }
}
