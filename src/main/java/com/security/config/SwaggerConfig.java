package com.security.config;

import java.util.List;

import org.springdoc.core.models.GroupedOpenApi;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.servers.Server;

@Configuration
public class SwaggerConfig {
    
    @Bean
    public OpenAPI customOpenAPI() {
        Server localServer = new io.swagger.v3.oas.models.servers.Server();
        localServer.setUrl("http://34.205.247.202:30999");
        localServer.setDescription("Servidor Local");
        
        return new OpenAPI()
                .info(new io.swagger.v3.oas.models.info.Info()
                        .title("API de Segurança com JWT")
                        .version("1.0")
                        .description("Documentação da API de segurança com JWT"))
                .servers(List.of(localServer))
                .components(new Components()
                        .addSecuritySchemes("bearerAuth", 
                                new SecurityScheme()
                                        .type(SecurityScheme.Type.HTTP)
                                        .scheme("bearer")
                                        .bearerFormat("JWT")
                        )
                );
    }
    
    @Bean
    public GroupedOpenApi allApi() {
        return GroupedOpenApi.builder()
                .group("all")
                .pathsToMatch("/**")
                .build();
    }
}