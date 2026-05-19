package br.com.lane.SpecRecon.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;
import java.util.Collections;

/**
 * Configuração CORS segura.
 * Permitir apenas domínios autorizados.
 */
@Configuration
public class CorsConfig {

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        
        // NUNCA usar "*" em produção - sempre lista explícita de domínios
        configuration.setAllowedOrigins(Arrays.asList(
            "http://localhost:3000",        // Frontend local
            "http://localhost:4200",        // Angular local
            "https://app.example.com",      // Produção
            "https://admin.example.com"     // Admin
        ));
        
        // Métodos HTTP permitidos
        configuration.setAllowedMethods(Arrays.asList(
            "GET", "POST", "PUT", "DELETE", "OPTIONS", "PATCH"
        ));
        
        // Headers permitidos
        configuration.setAllowedHeaders(Arrays.asList(
            "Content-Type",
            "Authorization",
            "X-Requested-With",
            "X-Signature",      // Para assinatura de payload
            "X-Timestamp"       // Para validação de timestamp
        ));
        
        // Headers que podem ser expostos ao cliente
        configuration.setExposedHeaders(Arrays.asList(
            "Content-Type",
            "Authorization",
            "X-Total-Count",    // Paginação
            "X-Page-Number"
        ));
        
        // Permitir credentials (cookies, auth headers)
        configuration.setAllowCredentials(true);
        
        // Cache CORS por 1 hora (3600 segundos)
        configuration.setMaxAge(3600L);
        
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        
        return source;
    }
}
