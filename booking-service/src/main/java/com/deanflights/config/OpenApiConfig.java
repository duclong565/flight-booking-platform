package com.deanflights.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * OpenAPI / Swagger configuration.
 *
 * <p>Declares a Bearer-JWT security scheme so Swagger UI shows an <b>Authorize</b> button:
 * log in via {@code POST /api/v1/auth/login}, click Authorize, paste the returned token, and
 * the protected endpoints (create flight, book) become click-testable in the browser.
 * The global security item attaches the token to requests once you've authorized.
 */
@Configuration
public class OpenApiConfig {

    private static final String SCHEME = "bearer-jwt";

    @Bean
    public OpenAPI bookingServiceOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Flight Booking Service API")
                        .version("v1")
                        .description("Search flights, authenticate, and create bookings. "
                                + "Log in at /api/v1/auth/login, click Authorize, and paste the token."))
                .components(new Components().addSecuritySchemes(SCHEME,
                        new SecurityScheme()
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("bearer")
                                .bearerFormat("JWT")))
                .addSecurityItem(new SecurityRequirement().addList(SCHEME));
    }
}
