package com.examradar.backend.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI examRadarOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("ExamRadar India REST API")
                        .description("Official examination and recruitment notification tracker REST API. " +
                                "Provides public catalogue search, unified examination calendar, candidate preferences, " +
                                "and administrative source management.")
                        .version("1.0.0")
                        .contact(new Contact()
                                .name("ExamRadar India Engineering")
                                .email("contact@examradar.in")
                                .url("https://examradar.in"))
                        .license(new License().name("Apache 2.0").url("https://www.apache.org/licenses/LICENSE-2.0")))
                .addSecurityItem(new SecurityRequirement().addList("bearerAuth"))
                .components(new Components()
                        .addSecuritySchemes("bearerAuth",
                                new SecurityScheme()
                                        .name("bearerAuth")
                                        .type(SecurityScheme.Type.HTTP)
                                        .scheme("bearer")
                                        .bearerFormat("JWT")
                                        .description("Supabase Auth JWT Token"))
                        .addSecuritySchemes("adminAuth",
                                new SecurityScheme()
                                        .name("X-Admin-Api-Key")
                                        .type(SecurityScheme.Type.APIKEY)
                                        .in(SecurityScheme.In.HEADER)
                                        .description("Administrative API Secret Header")));
    }
}
