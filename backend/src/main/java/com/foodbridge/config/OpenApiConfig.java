package com.foodbridge.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

/**
 * OpenAPI/Swagger documentation. Available at {@code /swagger-ui.html} and the
 * raw specification at {@code /api-docs}.
 */
@Configuration
public class OpenApiConfig {

    private static final String BEARER = "bearerAuth";

    @Bean
    public OpenAPI foodBridgeOpenApi() {
        return new OpenAPI()
                .info(new Info()
                        .title("FoodBridge API")
                        .version("1.0.0")
                        .description("""
                                REST API for FoodBridge - a food donation and distribution coordination platform
                                connecting food donors (restaurants, hotels, cafes) with verified NGOs.

                                **Authentication**: call `POST /api/auth/login`, then use the returned
                                `accessToken` as `Authorization: Bearer <token>`. Access tokens expire after
                                15 minutes; use `POST /api/auth/refresh` (refresh cookie) to obtain a new one.

                                **Roles**: DONOR publishes donations, NGO claims them (after admin verification),
                                ADMIN manages the platform.
                                """)
                        .contact(new Contact().name("FoodBridge").url("https://github.com/"))
                        .license(new License().name("MIT")))
                .servers(List.of(
                        new Server().url("http://localhost:8080").description("Local development")))
                .addSecurityItem(new SecurityRequirement().addList(BEARER))
                .components(new Components().addSecuritySchemes(BEARER,
                        new SecurityScheme()
                                .name(BEARER)
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("bearer")
                                .bearerFormat("JWT")
                                .description("Paste the access token returned by /api/auth/login")));
    }
}
