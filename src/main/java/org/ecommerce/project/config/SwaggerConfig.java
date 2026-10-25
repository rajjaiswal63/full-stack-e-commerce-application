package org.ecommerce.project.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class SwaggerConfig {
    @Bean
    public OpenAPI customOpenAPI(
            @Value("${spring.ecom.app.jwtCookieName:ecommerce}") String cookieName) {
        SecurityScheme cookieScheme = new SecurityScheme()
                .type(SecurityScheme.Type.APIKEY)
                .in(SecurityScheme.In.COOKIE)
                .name(cookieName)
                .description("JWT cookie set by the signin endpoint.");

        SecurityScheme bearerScheme = new SecurityScheme()
                .type(SecurityScheme.Type.HTTP)
                .scheme("bearer")
                .bearerFormat("JWT");

        return new OpenAPI()
                .info(new Info()
                        .title("E-commerce API")
                        .version("1.0.0")
                        .description("REST API for catalog, accounts, carts, checkout, orders, and analytics."))
                .components(new Components()
                        .addSecuritySchemes("JWT Cookie", cookieScheme)
                        .addSecuritySchemes("Bearer Authentication", bearerScheme));
    }
}
