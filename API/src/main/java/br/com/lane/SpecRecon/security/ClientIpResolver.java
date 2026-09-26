package br.com.lane.SpecRecon.security;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Resolve o IP real do cliente de forma segura.
 *
 * Hardening Sprint 3:
 *  Antes, cada classe lia o header X-Forwarded-For diretamente. Como esse header
 *  é enviado pelo próprio cliente, um atacante podia:
 *   - trocar o "IP" a cada requisição e nunca atingir o rate limit (bypass);
 *   - gravar um IP falso na trilha de auditoria.
 *
 *  Agora o X-Forwarded-For só é considerado quando a conexão vem de um proxy
 *  confiável cadastrado em app.security.trusted-proxies (ex.: IP do load balancer).
 *  Sem proxy configurado (padrão), usa sempre o IP da conexão TCP (getRemoteAddr).
 */
@Component
public class ClientIpResolver {

    private static volatile Set<String> trustedProxies = Set.of();

    @Value("${app.security.trusted-proxies:}")
    public void setTrustedProxies(String proxies) {
        // guardado em static para poder ser usado também fora de beans (mesmo padrão do EncryptedStringConverter)
        ClientIpResolver.trustedProxies = Arrays.stream(proxies.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .collect(Collectors.toUnmodifiableSet());
    }

    public static String resolve(HttpServletRequest request) {
        String remoteAddr = request.getRemoteAddr();

        if (trustedProxies.contains(remoteAddr)) {
            String forwardedFor = request.getHeader("X-Forwarded-For");
            if (forwardedFor != null && !forwardedFor.isBlank()) {
                return forwardedFor.split(",")[0].trim();
            }
        }
        return remoteAddr;
    }
}
