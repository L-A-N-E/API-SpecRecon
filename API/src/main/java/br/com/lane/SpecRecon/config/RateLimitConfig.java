package br.com.lane.SpecRecon.config;

import br.com.lane.SpecRecon.security.ClientIpResolver;
import br.com.lane.SpecRecon.service.AuditService;
import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.Bucket4j;
import io.github.bucket4j.ConsumptionProbe;
import io.github.bucket4j.Refill;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

/**
 * Rate Limiting (proteção contra abuso, brute force e DoS) com Bucket4j (token bucket).
 *
 * Hardening Sprint 3:
 *  - IP resolvido pelo ClientIpResolver (sem bypass via X-Forwarded-For falso).
 *  - Limite dedicado e mais rígido para /auth/** (anti brute force de login).
 *  - Header X-Rate-Limit-Remaining com o saldo REAL (antes era fixo "100").
 *  - Estouro de limite gera evento de auditoria RATE_LIMIT_EXCEEDED
 *    (no máximo 1 registro por IP por minuto, para o log não virar vetor de DoS).
 *
 * Limites configuráveis:
 *  - app.security.rate-limit.general-per-minute (padrão 100)
 *  - app.security.rate-limit.auth-per-minute    (padrão 10)
 */
@Configuration
public class RateLimitConfig implements WebMvcConfigurer {

    private final RateLimitInterceptor rateLimitInterceptor;

    public RateLimitConfig(RateLimitInterceptor rateLimitInterceptor) {
        this.rateLimitInterceptor = rateLimitInterceptor;
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(rateLimitInterceptor);
    }
}

@Component
class RateLimitInterceptor implements HandlerInterceptor {

    private static final Logger logger = LoggerFactory.getLogger(RateLimitInterceptor.class);
    private static final long AUDIT_COOLDOWN_MS = 60_000;

    private final Map<String, Bucket> generalBuckets = new ConcurrentHashMap<>();
    private final Map<String, Bucket> authBuckets = new ConcurrentHashMap<>();
    private final Map<String, Long> lastAudit = new ConcurrentHashMap<>();

    private final AuditService auditService;
    private final long generalPerMinute;
    private final long authPerMinute;

    RateLimitInterceptor(AuditService auditService,
                         @Value("${app.security.rate-limit.general-per-minute:100}") long generalPerMinute,
                         @Value("${app.security.rate-limit.auth-per-minute:10}") long authPerMinute) {
        this.auditService = auditService;
        this.generalPerMinute = generalPerMinute;
        this.authPerMinute = authPerMinute;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        String ip = ClientIpResolver.resolve(request);
        boolean isAuthEndpoint = request.getRequestURI().startsWith("/auth/");

        Bucket bucket = isAuthEndpoint
                ? authBuckets.computeIfAbsent(ip, k -> newBucket(authPerMinute))
                : generalBuckets.computeIfAbsent(ip, k -> newBucket(generalPerMinute));

        ConsumptionProbe probe = bucket.tryConsumeAndReturnRemaining(1);
        if (probe.isConsumed()) {
            response.addHeader("X-Rate-Limit-Remaining", String.valueOf(probe.getRemainingTokens()));
            return true;
        }

        long retryAfterSeconds = Math.max(1, TimeUnit.NANOSECONDS.toSeconds(probe.getNanosToWaitForRefill()));
        registerRateLimitEvent(ip, request.getRequestURI(), isAuthEndpoint);

        response.setStatus(429); // Too Many Requests
        response.setContentType("application/json");
        response.addHeader("Retry-After", String.valueOf(retryAfterSeconds));
        response.getWriter().write(
                "{\"status\": 429, \"error\": \"Too Many Requests\", \"message\": \"Limite de requisições excedido. Aguarde "
                        + retryAfterSeconds + " segundos.\", \"retryAfter\": " + retryAfterSeconds + "}"
        );
        return false;
    }

    /**
     * Loga (console/arquivo) todo estouro, mas grava no audit_log no máximo
     * 1 vez por minuto por IP, para um flood não lotar o banco.
     */
    private void registerRateLimitEvent(String ip, String uri, boolean isAuthEndpoint) {
        logger.warn("RATE_LIMIT_EXCEEDED ip={} uri={} scope={}", ip, uri, isAuthEndpoint ? "auth" : "general");

        long now = System.currentTimeMillis();
        Long previous = lastAudit.get(ip);
        if (previous == null || now - previous > AUDIT_COOLDOWN_MS) {
            lastAudit.put(ip, now);
            try {
                auditService.logAction("RATE_LIMIT_EXCEEDED", "API", 0L, ip,
                        "Limite de requisições excedido em " + uri
                                + (isAuthEndpoint ? " (possível brute force de login)" : ""),
                        ip, "BLOCKED");
            } catch (Exception e) {
                logger.error("Falha ao auditar rate limit: {}", e.getMessage());
            }
        }
    }

    private Bucket newBucket(long perMinute) {
        Bandwidth limit = Bandwidth.classic(perMinute, Refill.intervally(perMinute, Duration.ofMinutes(1)));
        return Bucket4j.builder().addLimit(limit).build();
    }
}
