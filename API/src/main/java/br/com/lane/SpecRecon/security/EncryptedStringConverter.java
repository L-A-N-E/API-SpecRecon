package br.com.lane.SpecRecon.security;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Converter JPA que criptografa Strings em repouso usando AES-256.
 * Aplicado via @Convert(converter = EncryptedStringConverter.class) no campo da entidade.
 *
 * Eixo 4 - Segurança de Dados e Privacidade:
 * "Criptografia de dados sensíveis em repouso"
 */
@Component
@Converter
public class EncryptedStringConverter implements AttributeConverter<String, String> {

    private static String encryptionKey;

    @Value("${app.security.data-encryption-key:}")
    public void setEncryptionKey(String key) {
        // Spring injeta no setter; guardamos em static para JPA poder usar
        // mesmo quando instancia o converter sem container.
        EncryptedStringConverter.encryptionKey = key;
    }

    @Override
    public String convertToDatabaseColumn(String attribute) {
        if (attribute == null || attribute.isEmpty()) {
            return attribute;
        }
        if (encryptionKey == null || encryptionKey.isEmpty()) {
            throw new IllegalStateException(
                    "DATA_ENCRYPTION_KEY não configurada. Defina app.security.data-encryption-key no .env"
            );
        }
        return DataEncryptionUtil.encrypt(attribute, encryptionKey);
    }

    @Override
    public String convertToEntityAttribute(String dbData) {
        if (dbData == null || dbData.isEmpty()) {
            return dbData;
        }
        if (encryptionKey == null || encryptionKey.isEmpty()) {
            throw new IllegalStateException(
                    "DATA_ENCRYPTION_KEY não configurada. Defina app.security.data-encryption-key no .env"
            );
        }
        return DataEncryptionUtil.decrypt(dbData, encryptionKey);
    }
}