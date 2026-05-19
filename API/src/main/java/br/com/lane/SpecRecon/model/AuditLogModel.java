package br.com.lane.SpecRecon.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import br.com.lane.SpecRecon.security.EncryptedStringConverter;
import jakarta.persistence.Convert;

import java.time.LocalDateTime;

/**
 * Entidade de auditoria para rastrear alterações em dados sensíveis.
 * Registra: o quê, quem, quando, onde, por que.
 */
@Entity
@Table(name = "audit_log")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class AuditLogModel {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Tipo de ação: CREATE, READ, UPDATE, DELETE, LOGIN, FAILED_LOGIN */
    @Column(nullable = false)
    private String action;

    /** Entidade afetada: User, Vehicle, Unit, etc */
    @Column(nullable = false)
    private String entityType;

    /** ID da entidade */
    @Column(nullable = false)
    private Long entityId;

    /** Usuário que realizou a ação */
    @Column(nullable = false)
    private String performedBy;

    /** Descrição da alteração (sem dados sensíveis) */
    @Column(columnDefinition = "TEXT")
    private String description;

    /** IP do cliente */
    @Column(nullable = false)
    private String ipAddress;

    /** Timestamp da ação */
    @Column(nullable = false)
    private LocalDateTime timestamp;

    /** Status da ação: SUCCESS, FAILED, UNAUTHORIZED */
    @Column(nullable = false)
    private String status;

    /** Detalhes adicionais (criptografados em repouso - AES-256) */
    @Column(columnDefinition = "TEXT")
    @Convert(converter = EncryptedStringConverter.class)
    private String details;

    @PrePersist
    public void prePersist() {
        this.timestamp = LocalDateTime.now();
    }
}
