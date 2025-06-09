package com.example.commonlib.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class SwaggerConfig {

    @Value("${spring.application.name}")
    private String applicationName;

    @Bean
    public OpenAPI openAPI() {
        Info info = new Info()
                .title(applicationName + " API Documentation")
                .version("v1.0")
                .description(applicationName + " REST API 명세서입니다.");

        // Basic Auth 스키마 설정
        SecurityScheme basicAuth = new SecurityScheme()
                .type(SecurityScheme.Type.HTTP)
                .scheme("basic");

        SecurityRequirement securityRequirement = new SecurityRequirement().addList("basicAuth");

        return new OpenAPI()
                .components(new Components().addSecuritySchemes("basicAuth", basicAuth))
                .addSecurityItem(securityRequirement)
                .info(info);
    }
} 