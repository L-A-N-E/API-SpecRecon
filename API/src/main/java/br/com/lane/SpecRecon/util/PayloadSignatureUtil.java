package br.com.lane.SpecRecon.util;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;

/**
 * Utilitário para clientes gerarem assinatura de payloads.
 * Use isso no seu frontend/mobile para assinar requisições.
 * 
 * Exemplo:
 * String payload = "{\"email\":\"test@example.com\",\"password\":\"Secret@123\",\"role\":\"ADMIN\"}";
 * String signature = PayloadSignatureUtil.generateSignature(payload, secretKey);
 * 
 * Depois envie no header:
 * X-Signature: <signature>
 */
public class PayloadSignatureUtil {

    private static final String ALGORITHM = "HmacSHA256";

    /**
     * Gera assinatura HMAC-SHA256 para um payload (para uso em clientes).
     * 
     * @param payload dados a assinar (JSON)
     * @param secretKey chave secreta compartilhada
     * @return assinatura em Base64
     */
    public static String generateSignature(String payload, String secretKey) {
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

    public static void main(String[] args) {
        // Teste de geração de assinatura
        String secretKey = "your-secret-key-min-32-characters-change-in-production";
        String payload = "{\"email\":\"john@example.com\",\"password\":\"SecurePass@123\",\"role\":\"ADMIN\"}";
        
        String signature = generateSignature(payload, secretKey);
        
        System.out.println("Payload: " + payload);
        System.out.println("Chave: " + secretKey);
        System.out.println("Assinatura: " + signature);
        System.out.println("\nUse no header X-Signature: " + signature);
    }
}
