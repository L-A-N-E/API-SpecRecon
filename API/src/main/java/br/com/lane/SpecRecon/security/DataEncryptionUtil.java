package br.com.lane.SpecRecon.security;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.Mac;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Base64;

/**
 * Utilitário para criptografar dados sensíveis em repouso.
 *
 * Algoritmo: AES-256-GCM (criptografia autenticada) com IV sintético.
 *
 * Por que IV sintético (determinístico)?
 *  - O e-mail é criptografado e o login faz findByEmail(...) + coluna UNIQUE.
 *    Para isso funcionar, o mesmo e-mail precisa gerar sempre o mesmo texto cifrado.
 *  - O IV é derivado via HMAC-SHA256(chaveIV, dado): mesmo dado -> mesmo IV;
 *    dados diferentes -> IVs diferentes. Assim nunca há reuso de IV com textos
 *    distintos (o que quebraria o GCM).
 *
 * Correção da Sprint 3 (achado do Semgrep "use-of-default-aes"):
 *  - Antes: Cipher.getInstance("AES") = AES/ECB/PKCS5Padding
 *    (sem IV, blocos iguais geram cifras iguais, sem verificação de integridade).
 *  - Agora: AES/GCM/NoPadding com tag de autenticação de 128 bits
 *    (qualquer adulteração no banco é detectada na leitura).
 *
 * Trade-off aceito e documentado: por ser determinístico, é possível saber que
 * dois registros têm o MESMO valor (necessário para busca/unicidade), mas não
 * qual é o valor.
 *
 * Formato armazenado: "v2:" + Base64( IV[12 bytes] || ciphertext || tag[16 bytes] )
 */
public final class DataEncryptionUtil {

    private static final String KEY_ALGORITHM = "AES";
    private static final String TRANSFORMATION = "AES/GCM/NoPadding";
    private static final String HMAC_ALGORITHM = "HmacSHA256";
    private static final String FORMAT_PREFIX = "v2:";
    private static final byte[] IV_KEY_CONTEXT = "SpecRecon-SIV-v2".getBytes(StandardCharsets.UTF_8);

    private static final int KEY_SIZE_BITS = 256;
    private static final int IV_LENGTH_BYTES = 12;     // recomendado pelo NIST para GCM
    private static final int TAG_LENGTH_BITS = 128;

    private DataEncryptionUtil() {
    }

    /**
     * Gera chave de criptografia AES-256 em Base64 (para colocar no .env).
     */
    public static String generateKey() {
        try {
            KeyGenerator keyGenerator = KeyGenerator.getInstance(KEY_ALGORITHM);
            keyGenerator.init(KEY_SIZE_BITS);
            SecretKey secretKey = keyGenerator.generateKey();
            return Base64.getEncoder().encodeToString(secretKey.getEncoded());
        } catch (Exception e) {
            throw new IllegalStateException("Erro ao gerar chave de criptografia", e);
        }
    }

    /**
     * Criptografa dados sensíveis.
     *
     * @param data       dados a criptografar
     * @param encodedKey chave AES-256 em Base64
     * @return "v2:" + Base64(IV || ciphertext || tag)
     */
    public static String encrypt(String data, String encodedKey) {
        try {
            byte[] key = decodeKey(encodedKey);
            byte[] plaintext = data.getBytes(StandardCharsets.UTF_8);
            byte[] iv = syntheticIv(key, plaintext);

            Cipher cipher = Cipher.getInstance(TRANSFORMATION);
            cipher.init(Cipher.ENCRYPT_MODE,
                    new SecretKeySpec(key, KEY_ALGORITHM),
                    new GCMParameterSpec(TAG_LENGTH_BITS, iv));
            byte[] cipherText = cipher.doFinal(plaintext);

            byte[] output = ByteBuffer.allocate(iv.length + cipherText.length)
                    .put(iv)
                    .put(cipherText)
                    .array();
            return FORMAT_PREFIX + Base64.getEncoder().encodeToString(output);
        } catch (IllegalArgumentException e) {
            throw e;
        } catch (Exception e) {
            throw new IllegalStateException("Erro ao criptografar dados", e);
        }
    }

    /**
     * Descriptografa e valida a integridade (tag GCM).
     *
     * @param encryptedData valor no formato "v2:..."
     * @param encodedKey    chave AES-256 em Base64
     * @return dados em texto claro
     */
    public static String decrypt(String encryptedData, String encodedKey) {
        if (encryptedData == null || !encryptedData.startsWith(FORMAT_PREFIX)) {
            throw new IllegalStateException(
                    "Formato de dado criptografado desconhecido (esperado v2/AES-GCM). "
                            + "Dados gravados com a versão antiga (AES-ECB) precisam ser recriados.");
        }
        try {
            byte[] key = decodeKey(encodedKey);
            byte[] input = Base64.getDecoder().decode(encryptedData.substring(FORMAT_PREFIX.length()));
            if (input.length <= IV_LENGTH_BYTES) {
                throw new IllegalStateException("Dado criptografado inválido (tamanho)");
            }

            byte[] iv = Arrays.copyOfRange(input, 0, IV_LENGTH_BYTES);
            byte[] cipherText = Arrays.copyOfRange(input, IV_LENGTH_BYTES, input.length);

            Cipher cipher = Cipher.getInstance(TRANSFORMATION);
            cipher.init(Cipher.DECRYPT_MODE,
                    new SecretKeySpec(key, KEY_ALGORITHM),
                    new GCMParameterSpec(TAG_LENGTH_BITS, iv));
            byte[] plaintext = cipher.doFinal(cipherText);   // lança AEADBadTagException se adulterado

            return new String(plaintext, StandardCharsets.UTF_8);
        } catch (IllegalStateException e) {
            throw e;
        } catch (Exception e) {
            throw new IllegalStateException("Erro ao descriptografar dados (dado corrompido ou adulterado)", e);
        }
    }

    /**
     * IV sintético: HMAC-SHA256(chaveIV, dado), truncado para 12 bytes.
     * A chaveIV é derivada da chave principal, para não reutilizar a mesma chave
     * em dois algoritmos diferentes.
     */
    private static byte[] syntheticIv(byte[] key, byte[] plaintext) throws Exception {
        Mac kdf = Mac.getInstance(HMAC_ALGORITHM);
        kdf.init(new SecretKeySpec(key, HMAC_ALGORITHM));
        byte[] ivKey = kdf.doFinal(IV_KEY_CONTEXT);

        Mac mac = Mac.getInstance(HMAC_ALGORITHM);
        mac.init(new SecretKeySpec(ivKey, HMAC_ALGORITHM));
        return Arrays.copyOf(mac.doFinal(plaintext), IV_LENGTH_BYTES);
    }

    private static byte[] decodeKey(String encodedKey) {
        byte[] key = Base64.getDecoder().decode(encodedKey);
        if (key.length != KEY_SIZE_BITS / 8) {
            throw new IllegalArgumentException("DATA_ENCRYPTION_KEY deve ter 256 bits (32 bytes em Base64)");
        }
        return key;
    }
}
