package com.example.commonlib.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springdoc.core.GroupedOpenApi;
import org.springdoc.core.customizers.OpenApiCustomiser;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
public class SwaggerConfig {

    @Value("${spring.application.name}")
    private String applicationName;

    @Bean
    public OpenAPI customOpenAPI() {
        final String securitySchemeName = "Authorization";
        
        Info info = new Info()
                .title(applicationName + " API Documentation")
                .version("v1.0")
                .description(applicationName + " REST API 명세서입니다.");

        // JWT Bearer 토큰 스키마 설정
        SecurityScheme securityScheme = new SecurityScheme()
                .name(securitySchemeName)
                .type(SecurityScheme.Type.HTTP)
                .scheme("bearer")
                .bearerFormat("JWT")
                .in(SecurityScheme.In.HEADER)
                .description("JWT Bearer 토큰을 입력해주세요. 예: Bearer {token}");

        return new OpenAPI()
                .components(new Components().addSecuritySchemes(securitySchemeName, securityScheme))
                .info(info)
                .security(List.of(new SecurityRequirement().addList("bearerAuth")));
    }

} 