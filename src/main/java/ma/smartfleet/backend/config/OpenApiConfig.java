package ma.smartfleet.backend.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.Components;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI smartFleetOpenAPI() {
        return new OpenAPI()
            .info(new Info()
                .title("SmartFleet Backend API")
                .description("API de gestion logistique SmartFleet — livraisons, routage, tracking GPS")
                .version("1.0.0")
                .contact(new Contact()
                    .name("SmartFleet Team")
                    .email("contact@smartfleet.ma")))
            .addSecurityItem(new SecurityRequirement().addList("Bearer Auth"))
            .components(new Components()
                .addSecuritySchemes("Bearer Auth", new SecurityScheme()
                    .name("Bearer Auth")
                    .type(SecurityScheme.Type.HTTP)
                    .scheme("bearer")
                    .bearerFormat("JWT")
                    .description("Token JWT — format : Bearer {token}")));
    }
}