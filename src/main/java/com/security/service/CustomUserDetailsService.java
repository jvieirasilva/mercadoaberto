package com.security.service;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import com.security.repository.UserRepository;

@Service
public class CustomUserDetailsService implements UserDetailsService {

    @Autowired
    private UserRepository userRepository;

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        var user = userRepository.findByEmail(username)
            .orElseThrow(() -> new UsernameNotFoundException("Usuário não encontrado: " + username));

        return new org.springframework.security.core.userdetails.User(
            user.getEmail(),
            user.getPassword(),
            user.isActive(),
            true,
            true,
            user.isNotLocked(), // ⛔ Aqui é o que dispara o LockedException
            //user.get.getAuthorities()
            List.of(new SimpleGrantedAuthority("ROLE_" + user.getRole().name()))
        );
    }
}
