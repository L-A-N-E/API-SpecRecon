package br.com.lane.SpecRecon.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Configuração central de segurança da web.
 * Registra interceptors de rate limiting, CORS e validação de assinatura.
 */
@Configuration
public class SecurityConfig implements WebMvcConfigurer {

    @Autowired
    private PayloadSignatureInterceptor payloadSignatureInterceptor;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        // Rate Limiting é registrado em RateLimitConfig
        
        // Assinatura de Payload
        registry.addInterceptor(payloadSignatureInterceptor)
                .addPathPatterns("/users/**", "/vehicles/**", "/units/**", "/specification-types/**");
    }
}
