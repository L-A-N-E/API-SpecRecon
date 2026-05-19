package br.com.lane.SpecRecon.security;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;

/**
 * Gerenciador de assinatura de payloads usando HMAC-SHA256.
 * Garante integridade dos dados em trânsito.
 * 
 * Uso:
 * 1. Cliente assina o payload com chave secreta
 * 2. Envia no header X-Signature
 * 3. Servidor valida a assinatura
 * 4. Se inválida, rejeita a requisição
 */
@Component
public class PayloadSignatureManager {

    @Value("${app.security.payload-signature-key:}")
    private String secretKey;

    private static final String ALGORITHM = "HmacSHA256";

    /**
     * Gera assinatura HMAC-SHA256 para um payload.
     * 
     * @param payload dados a assinar
     * @return assinatura em Base64
     */
    public String generateSignature(String payload) {
        if (secretKey == null || secretKey.isBlank() || secretKey.length() < 32) {
            throw new IllegalStateException(
                    "PAYLOAD_SIGNATURE_KEY ausente ou fraco. Defina app.security.payload-signature-key no .env com no mínimo 32 caracteres."
            );
        }
        try {
            Mac mac = Mac.getInstance(ALGORITHM);
            SecretKeySpec secretKeySpec = new SecretKeySpec(
                secretKey.getBytes(StandardCharsets.UTF_8),
                0,
                secretKey.getBytes(StandardCharsets.UTF_8).length,
                ALGORITHM
            );
            mac.init(secretKeySpec);
            
            byte[] hmac = mac.doFinal(payload.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(hmac);
        } catch (NoSuchAlgorithmException | InvalidKeyException e) {
            throw new RuntimeException("Erro ao gerar assinatura de payload", e);
        }
    }

    /**
     * Valida se a assinatura fornecida corresponde ao payload.
     * 
     * @param payload dados originais
     * @param providedSignature assinatura fornecida (header X-Signature)
     * @return true se válida, false caso contrário
     */
    public boolean validateSignature(String payload, String providedSignature) {
        if (providedSignature == null || providedSignature.isEmpty()) {
            return false;
        }
        
        String generatedSignature = generateSignature(payload);
        
        // Usar comparação constante para evitar timing attacks
        return constantTimeEquals(generatedSignature, providedSignature);
    }

    /**
     * Comparação segura contra timing attacks.
     * Compara todas as posições mesmo que falhe.
     */
    private boolean constantTimeEquals(String a, String b) {
        if (a == null || b == null) {
            return a == b;
        }
        
        byte[] aBytes = a.getBytes(StandardCharsets.UTF_8);
        byte[] bBytes = b.getBytes(StandardCharsets.UTF_8);
        
        if (aBytes.length != bBytes.length) {
            return false;
        }
        
        int result = 0;
        for (int i = 0; i < aBytes.length; i++) {
            result |= aBytes[i] ^ bBytes[i];
        }
        
        return result == 0;
    }
}
