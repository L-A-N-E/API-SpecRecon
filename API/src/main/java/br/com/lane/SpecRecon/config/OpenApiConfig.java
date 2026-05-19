package br.com.lane.SpecRecon.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.parameters.Parameter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("SpecRecon API")
                        .version("1.0")
                        .description("Documentação da API para o Challenge da FORD"))
                .addSecurityItem(new SecurityRequirement().addList("Bearer Authentication"))
                .components(new Components()
                        .addSecuritySchemes("Bearer Authentication", new SecurityScheme()
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("bearer")
                                .bearerFormat("JWT")
                                .description("Cole o token JWT obtido no /auth/login"))
                        .addParameters("X-Signature", new Parameter()
                                .in("header")
                                .name("X-Signature")
                                .description("Assinatura HMAC-SHA256 do payload. Obrigatório em POST/PUT/PATCH.")
                                .required(false)
                                .schema(new io.swagger.v3.oas.models.media.StringSchema())));
    }
}