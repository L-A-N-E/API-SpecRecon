package br.com.lane.SpecRecon.controller;

import br.com.lane.SpecRecon.service.AuditService;
import io.swagger.v3.oas.annotations.Hidden;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Enumeration;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Honeypot Controller — endpoints falsos para atrair e registrar atacantes.
 * @Hidden esconde do Swagger — atacante não sabe que existe.
 */
@Hidden
@RestController
public class HoneypotController {

    private static final Logger logger = LoggerFactory.getLogger(HoneypotController.class);
    private static final String RICKROLL_HTML = """
    <!DOCTYPE html>
    <html>
    <head>
        <title>🔒 Admin Panel — SpecRecon</title>
        <style>
            body { margin: 0; background: #000; display: flex; 
                   flex-direction: column; align-items: center; 
                   justify-content: center; height: 100vh; color: white;
                   font-family: monospace; }
            h1 { margin-bottom: 1rem; }
        </style>
    </head>
    <body>
        <h1>🍯 Get RickRolled!</h1>
        <iframe width="800" height="450"
            src="https://www.youtube.com/embed/dQw4w9WgXcQ?autoplay=1"
            allow="autoplay"
            frameborder="0"
            allowfullscreen>
        </iframe>
    </body>
    </html>
    """;

    @Autowired
    private AuditService auditService;

    @GetMapping("/.env")
    public ResponseEntity<?> fakeEnv(HttpServletRequest request) {
        return honeypot(request, "/.env", "DB_PASS=admin123\nJWT_SECRET=supersecret\nAPI_KEY=sk-fake-key-haha");
    }

    @GetMapping("/admin")
    public ResponseEntity<?> fakeAdmin(HttpServletRequest request) {
        return honeypot(request, "/admin", null);
    }

    @GetMapping("/admin/debug")
    public ResponseEntity<?> fakeDebug(HttpServletRequest request) {
        return honeypot(request, "/admin/debug", null);
    }

    @GetMapping("/actuator")
    public ResponseEntity<?> fakeActuator(HttpServletRequest request) {
        return honeypot(request, "/actuator", null);
    }

    @GetMapping("/actuator/env")
    public ResponseEntity<?> fakeActuatorEnv(HttpServletRequest request) {
        return honeypot(request, "/actuator/env", null);
    }

    @GetMapping("/api/v1/internal")
    public ResponseEntity<?> fakeInternal(HttpServletRequest request) {
        return honeypot(request, "/api/v1/internal", null);
    }

    @GetMapping("/config")
    public ResponseEntity<?> fakeConfig(HttpServletRequest request) {
        return honeypot(request, "/config", null);
    }

    @GetMapping("/backup")
    public ResponseEntity<?> fakeBackup(HttpServletRequest request) {
        return honeypot(request, "/backup", null);
    }

    @PostMapping("/admin/login")
    public ResponseEntity<?> fakeAdminLogin(HttpServletRequest request) {
        return honeypot(request, "/admin/login", null);
    }

    // --- lógica central ---

    private ResponseEntity<?> honeypot(HttpServletRequest request, String endpoint, String fakeResponse) {
        String ip = getClientIp(request);
        String userAgent = request.getHeader("User-Agent");
        String headers = collectHeaders(request);

        // Log bem visível no console
        logger.warn("🍯 ═══════════════════════════════════════════════");
        logger.warn("🍯  HONEYPOT ATTACK DETECTED!");
        logger.warn("🍯  Endpoint  : {}", endpoint);
        logger.warn("🍯  IP        : {}", ip);
        logger.warn("🍯  User-Agent: {}", userAgent);
        logger.warn("🍯  Time      : {}", java.time.LocalDateTime.now());
        logger.warn("🍯  Headers   : {}", headers);
        logger.warn("🍯 ═══════════════════════════════════════════════");

        // Auditoria completa
        auditService.logAction(
                "HONEYPOT_HIT",
                "Honeypot",
                0L,
                ip,
                "Acesso a endpoint falso: " + endpoint + " | UA: " + userAgent + " | Headers: " + headers,
                ip,
                "SUSPICIOUS"
        );

        // Se for /.env, retorna dados falsos convincentes antes do redirect
        if (fakeResponse != null) {
            HttpHeaders responseHeaders = new HttpHeaders();
            responseHeaders.add("Content-Type", "text/plain");
            return ResponseEntity.ok().headers(responseHeaders).body(fakeResponse);
        }

        // Para os outros, serve a página HTML com autoplay
        HttpHeaders responseHeaders = new HttpHeaders();
        responseHeaders.add("Content-Type", "text/html; charset=UTF-8");
        return ResponseEntity.ok().headers(responseHeaders).body(RICKROLL_HTML);
    }

    private String getClientIp(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isEmpty()) {
            return forwarded.split(",")[0].trim();
        }
        String realIp = request.getHeader("X-Real-IP");
        if (realIp != null && !realIp.isEmpty()) {
            return realIp;
        }
        return request.getRemoteAddr();
    }

    private String collectHeaders(HttpServletRequest request) {
        Map<String, String> headers = new LinkedHashMap<>();
        Enumeration<String> names = request.getHeaderNames();
        while (names.hasMoreElements()) {
            String name = names.nextElement();
            // Nunca logar Authorization completo — apenas indicar presença
            if (name.equalsIgnoreCase("authorization")) {
                headers.put(name, "[PRESENT]");
            } else {
                headers.put(name, request.getHeader(name));
            }
        }
        return headers.toString();
    }
}