package com.ilynkin.coding_assignment.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class SwaggerConfig {

    @Bean
    public OpenAPI openAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("MeterHub — Сервис управления приборами учёта электроэнергии")
                        .description("""
                                REST API для управления приборами учёта электроэнергии и передачи показаний.
                                
                                ## Роли
                                - **ADMIN** — полный доступ: управление пользователями, приборами учёта, показаниями
                                - **MANAGER** — управление показаниями приборов учёта
                                
                                ## Аутентификация
                                Для доступа к защищённым endpoint'ам необходимо передавать Bearer-токен
                                в заголовке: `Authorization: Bearer <токен>`
                                """)
                        .version("1.0.0")
                        .contact(new Contact().name("Provision Team"))
                )
                .addSecurityItem(new SecurityRequirement().addList("BearerAuth"))
                .schemaRequirement("BearerAuth", new SecurityScheme()
                        .name("BearerAuth")
                        .type(SecurityScheme.Type.HTTP)
                        .scheme("bearer")
                        .bearerFormat("UUID"));
    }
}
