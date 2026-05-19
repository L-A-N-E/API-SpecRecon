package br.com.lane.SpecRecon.security;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;

/**
 * Anonimização e pseudonimização de PII (Eixo 4).
 *
 * - Anonimização (irreversível): mascarar para exibição em logs, dashboards.
 * - Pseudonimização (reversível com chave): hash determinístico para joins
 *   em pipelines de ML mantendo a possibilidade de reidentificar via lookup.
 */
@Component
public class PiiAnonymizer {

    private static String pseudonymizationSalt;

    @Value("${app.security.pseudonymization-salt:}")
    public void setPseudonymizationSalt(String salt) {
        PiiAnonymizer.pseudonymizationSalt = salt;
    }

    /**
     * Mascara um e-mail preservando primeira letra e domínio.
     * Ex: "joao.silva@empresa.com" -> "j***@empresa.com"
     */
    public static String maskEmail(String email) {
        if (email == null || !email.contains("@")) return "***";
        String[] parts = email.split("@", 2);
        String local = parts[0];
        String domain = parts[1];
        String maskedLocal = local.isEmpty() ? "***" : local.charAt(0) + "***";
        return maskedLocal + "@" + domain;
    }

    /**
     * Mascara CPF/documento mostrando só os 3 primeiros e 2 últimos dígitos.
     * Ex: "12345678900" -> "123******00"
     */
    public static String maskDocument(String document) {
        if (document == null || document.length() < 5) return "***";
        String digits = document.replaceAll("\\D", "");
        if (digits.length() < 5) return "***";
        return digits.substring(0, 3) + "******" + digits.substring(digits.length() - 2);
    }

    /**
     * Mascara telefone mostrando só os 4 últimos dígitos.
     * Ex: "+5511987654321" -> "*******4321"
     */
    public static String maskPhone(String phone) {
        if (phone == null || phone.length() < 4) return "***";
        String digits = phone.replaceAll("\\D", "");
        if (digits.length() < 4) return "***";
        return "*******" + digits.substring(digits.length() - 4);
    }

    /**
     * Reduz nome para iniciais. Ex: "João da Silva" -> "J. d. S."
     */
    public static String maskName(String name) {
        if (name == null || name.isBlank()) return "***";
        String[] parts = name.trim().split("\\s+");
        StringBuilder sb = new StringBuilder();
        for (String p : parts) {
            if (!p.isEmpty()) sb.append(p.charAt(0)).append(". ");
        }
        return sb.toString().trim();
    }

    /**
     * Pseudonimização determinística via SHA-256(value + salt).
     * Mesmo input gera mesmo output (permite joins em ML),
     * mas não é reversível sem lookup table externa.
     *
     * Ex: "joao@empresa.com" -> "USR_a8f3d2c1..."
     */
    public static String pseudonymize(String value) {
        if (value == null || value.isEmpty()) return null;
        if (pseudonymizationSalt == null || pseudonymizationSalt.isEmpty()) {
            throw new IllegalStateException(
                    "PSEUDONYMIZATION_SALT não configurado. Defina app.security.pseudonymization-salt no .env"
            );
        }
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest((value + pseudonymizationSalt).getBytes(StandardCharsets.UTF_8));
            return "USR_" + HexFormat.of().formatHex(hash).substring(0, 16);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256 indisponível", e);
        }
    }
}