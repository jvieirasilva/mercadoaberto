package com.security.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/**")
                .allowedOrigins(
                    // Desenvolvimento local
                    "http://localhost:3000",
                    "http://localhost:4200",
                    "http://localhost:5173",
                    "http://localhost:8080",
                    "http://localhost:18083",
                    "http://127.0.0.1:3000",
                    "http://192.168.49.2:30999",
                    
                    // IPs EC2 (adicione TODOS os IPs que você usa)
                    "http://3.235.4.11:8080",
                    "http://44.223.13.157:8080",
                    "http://34.205.247.202:8080",
                    "http://18.207.101.195:8080", 
                    "http://3.234.205.170:8080",

                    // Produção AWS
                    "https://main.d1zwzlsm520sqr.amplifyapp.com",
                    "https://zl0pbu4u85.execute-api.us-east-1.amazonaws.com"
                )
                .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")
                .allowedHeaders("*")
                .exposedHeaders("Authorization", "Refresh-Token")
                .allowCredentials(true)
                .maxAge(3600);
    }
}