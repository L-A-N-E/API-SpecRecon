package br.com.lane.SpecRecon.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import br.com.lane.SpecRecon.security.EncryptedStringConverter;

/**
 * Entidade de domínio para usuários.
 * Representa credenciais e perfil do usuário no sistema.
 */
@Entity
@Table(name = "User")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class UserModel {
    /** Identificador do usuário. */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * E-mail do usuário (único).
     * Armazenado criptografado com AES-256 via EncryptedStringConverter.
     */
    @Convert(converter = EncryptedStringConverter.class)
    @Column(nullable = false, unique = true, length = 512)
    private String email;

    /** Senha do usuário armazenada como hash BCrypt */
    @Column(nullable = false)
    private String password;

    /** Perfil do usuário. */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Role role;
}
