package br.com.lane.SpecRecon.config;

import br.com.lane.SpecRecon.security.PayloadSignatureManager;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;
import br.com.lane.SpecRecon.security.CachedBodyHttpServletRequest;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.BufferedReader;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * Interceptor para validação de assinatura de payloads (Eixo 3).
 * Em endpoints SENSÍVEIS, exige header X-Signature; em outros, é opcional.
 */
@Component
public class PayloadSignatureInterceptor implements HandlerInterceptor {

    @Autowired
    private PayloadSignatureManager signatureManager;

    /**
     * Endpoints sensíveis: requerem assinatura HMAC obrigatória.
     * Qualquer POST/PUT/PATCH nesses prefixos precisa do header X-Signature.
     */
    private static final List<String> SENSITIVE_PREFIXES = List.of(
            "/users",
            "/vehicles",
            "/units",
            "/specification-types"
    );

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        String method = request.getMethod();
        if (!method.matches("POST|PUT|PATCH")) {
            return true;
        }

        String uri = request.getRequestURI();
        if (uri.contains("/swagger") || uri.contains("/v3/api-docs")) {
            return true;
        }

        boolean isSensitive = SENSITIVE_PREFIXES.stream().anyMatch(uri::startsWith);
        String providedSignature = request.getHeader("X-Signature");

        if (providedSignature == null || providedSignature.isEmpty()) {
            if (isSensitive) {
                response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                response.setContentType("application/json");
                response.getWriter().write(
                        "{\"status\": 401, \"error\": \"Unauthorized\", \"message\": \"Header X-Signature é obrigatório para este endpoint\"}"
                );
                return false;
            }
            return true; // não-sensível segue sem assinatura
        }

        try {
            String payload = readRequestBody(request);

            if (!signatureManager.validateSignature(payload, providedSignature)) {
                response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                response.setContentType("application/json");
                response.getWriter().write(
                        "{\"status\": 401, \"error\": \"Unauthorized\", \"message\": \"Assinatura de payload inválida\"}"
                );
                return false;
            }

            return true;
        } catch (IOException e) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            response.setContentType("application/json");
            response.getWriter().write(
                    "{\"status\": 400, \"error\": \"Bad Request\", \"message\": \"Erro ao validar assinatura de payload\"}"
            );
            return false;
        }
    }

    private String readRequestBody(HttpServletRequest request) throws IOException {
        if (request instanceof CachedBodyHttpServletRequest cached) {
            return new String(cached.getCachedBody(), StandardCharsets.UTF_8);
        }
        // Fallback (não deveria acontecer pois RequestBodyCachingFilter envolve tudo)
        BufferedReader reader = request.getReader();
        StringBuilder sb = new StringBuilder();
        String line;
        while ((line = reader.readLine()) != null) {
            sb.append(line);
        }
        return sb.toString();
    }
}