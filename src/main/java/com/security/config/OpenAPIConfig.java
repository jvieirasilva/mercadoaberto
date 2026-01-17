package com.security.config;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeIn;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.info.Contact;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.info.License;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.security.SecurityScheme;
import io.swagger.v3.oas.annotations.servers.Server;

@OpenAPIDefinition(
        info = @Info(
                contact = @Contact(
                        name = "Suporte",
                        email = "suporte@exemplo.com",
                        url = "https://exemplo.com"
                ),
                description = "Documentação da API de segurança com JWT",
                title = "API de Segurança com JWT",
                version = "1.0",
                license = @License(
                        name = "Licença da API",
                        url = "https://exemplo.com/licenca"
                ),
                termsOfService = "https://exemplo.com/termos"
        ),
        servers = {
                @Server(
                        description = "AWS Production",
                        url = "http://44.201.53.3:8080"  // ⬅️ ATUALIZE AQUI
                ),
                @Server(
                        description = "Minikube Local",
                        url = "http://192.168.49.2:30999"
                ),
                @Server(
                        description = "Local Development",
                        url = "http://localhost:8080"
                )
        },
        security = {
                @SecurityRequirement(
                        name = "bearerAuth"
                )
        }
)
@SecurityScheme(
        name = "bearerAuth",
        description = "JWT auth description",
        scheme = "bearer",
        type = SecuritySchemeType.HTTP,
        bearerFormat = "JWT",
        in = SecuritySchemeIn.HEADER
)
public class OpenAPIConfig {
}