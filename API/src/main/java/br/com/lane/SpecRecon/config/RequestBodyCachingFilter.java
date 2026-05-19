package br.com.lane.SpecRecon.config;

import br.com.lane.SpecRecon.security.CachedBodyHttpServletRequest;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * Filtro que envolve cada request POST/PUT/PATCH em um CachedBodyHttpServletRequest,
 * permitindo que o body seja lido múltiplas vezes (interceptor + controller).
 * Roda ANTES do JwtAuthenticationFilter e do PayloadSignatureInterceptor.
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class RequestBodyCachingFilter extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        String method = request.getMethod();
        if (method.matches("POST|PUT|PATCH")) {
            CachedBodyHttpServletRequest wrapped = new CachedBodyHttpServletRequest(request);
            chain.doFilter(wrapped, response);
        } else {
            chain.doFilter(request, response);
        }
    }
}