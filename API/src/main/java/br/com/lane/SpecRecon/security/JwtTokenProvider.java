package br.com.lane.SpecRecon.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

/**
 * Gerenciador de JWT (JSON Web Tokens).
 * Responsável por gerar, validar e extrair informações de tokens.
 *
 * Hardening Sprint 3:
 *  - Sem secret padrão hardcoded: a API NÃO sobe se JWT_SECRET estiver ausente ou fraco.
 *  - Claim "type" separa access token (1h) de refresh token (24h), impedindo que um
 *    refresh token roubado seja usado como access token.
 */
@Component
public class JwtTokenProvider {

    public static final String CLAIM_TYPE = "type";
    public static final String TYPE_ACCESS = "access";
    public static final String TYPE_REFRESH = "refresh";

    /** HS512 exige chave de pelo menos 512 bits (64 bytes). */
    private static final int MIN_SECRET_BYTES = 64;

    @Value("${app.security.jwt-secret:}")
    private String jwtSecret;

    @Value("${app.security.jwt-expiration:3600000}")  // 1 hora em ms
    private long jwtExpirationMs;

    @Value("${app.security.jwt-refresh-expiration:86400000}")  // 24 horas em ms
    private long jwtRefreshExpirationMs;

    private SecretKey signingKey;

    /**
     * Fail-fast: valida o secret na inicialização da aplicação.
     */
    @PostConstruct
    void init() {
        if (jwtSecret == null || jwtSecret.isBlank()) {
            throw new IllegalStateException(
                    "JWT_SECRET não configurado. Defina app.security.jwt-secret no .env");
        }
        byte[] secretBytes = jwtSecret.getBytes(StandardCharsets.UTF_8);
        if (secretBytes.length < MIN_SECRET_BYTES) {
            throw new IllegalStateException(
                    "JWT_SECRET fraco: HS512 exige no mínimo " + MIN_SECRET_BYTES + " bytes (512 bits)");
        }
        this.signingKey = Keys.hmacShaKeyFor(secretBytes);
    }

    /**
     * Gera access token (curta duração) com informações do usuário.
     */
    public String generateToken(UserDetails userDetails, Long userId, String role) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("userId", userId);
        claims.put("role", role);
        claims.put(CLAIM_TYPE, TYPE_ACCESS);

        return createToken(claims, userDetails.getUsername(), jwtExpirationMs);
    }

    /**
     * Gera refresh token (longa duração), usado SOMENTE em /auth/refresh.
     */
    public String generateRefreshToken(UserDetails userDetails, Long userId) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("userId", userId);
        claims.put(CLAIM_TYPE, TYPE_REFRESH);

        return createToken(claims, userDetails.getUsername(), jwtRefreshExpirationMs);
    }

    /**
     * Valida se o token JWT é válido (assinatura, expiração e usuário).
     */
    public boolean validateToken(String token, UserDetails userDetails) {
        try {
            String username = extractUsername(token);
            return username.equals(userDetails.getUsername()) && !isTokenExpired(token);
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * true somente para access tokens (claim type=access).
     */
    public boolean isAccessToken(String token) {
        return TYPE_ACCESS.equals(extractAllClaims(token).get(CLAIM_TYPE, String.class));
    }

    /**
     * true somente para refresh tokens (claim type=refresh).
     */
    public boolean isRefreshToken(String token) {
        return TYPE_REFRESH.equals(extractAllClaims(token).get(CLAIM_TYPE, String.class));
    }

    /**
     * Extrai username (email) do token.
     */
    public String extractUsername(String token) {
        return extractAllClaims(token).getSubject();
    }

    /**
     * Extrai role (papel) do token.
     */
    public String extractRole(String token) {
        return (String) extractAllClaims(token).get("role");
    }

    /**
     * Extrai userId do token.
     */
    public Long extractUserId(String token) {
        return ((Number) extractAllClaims(token).get("userId")).longValue();
    }

    /**
     * Verifica se o token expirou.
     */
    public boolean isTokenExpired(String token) {
        Date expiration = extractAllClaims(token).getExpiration();
        return expiration.before(new Date());
    }

    /**
     * Extrai todas as claims do token (valida assinatura e expiração).
     */
    private Claims extractAllClaims(String token) {
        return Jwts.parser()
                .setSigningKey(signingKey)
                .build()
                .parseClaimsJws(token)
                .getBody();
    }

    private String createToken(Map<String, Object> claims, String subject, long expirationTime) {
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + expirationTime);

        return Jwts.builder()
                .setClaims(claims)
                .setSubject(subject)
                .setIssuedAt(now)
                .setExpiration(expiryDate)
                .signWith(signingKey, SignatureAlgorithm.HS512)
                .compact();
    }
}
