package com.security;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.kafka.annotation.EnableKafka;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import com.security.repository.UserRepository;

/*
 José Vieira da Silva Neto 
 */
@SpringBootApplication
//@EnableKafka
public class SecurityApplication {

    public static void main(String[] args) {
        SpringApplication.run(SecurityApplication.class, args);
    }

    @Bean
    public UserDetailsService userDetailsService(UserRepository repository) {
        return username -> repository.findByEmail(username)
                .orElseThrow(() -> new UsernameNotFoundException("User not found"));
    }
}