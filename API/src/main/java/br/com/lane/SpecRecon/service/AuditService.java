package br.com.lane.SpecRecon.service;

import br.com.lane.SpecRecon.model.AuditLogModel;
import br.com.lane.SpecRecon.repository.AuditLogRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Serviço de auditoria.
 * Registra todas as ações críticas no sistema.
 */
@Service
public class AuditService {

    @Autowired
    private AuditLogRepository auditLogRepository;

    /**
     * Registra ação de auditoria.
     */
    public void logAction(String action, String entityType, Long entityId, 
                         String performedBy, String description, String ipAddress, String status) {
        AuditLogModel auditLog = new AuditLogModel();
        auditLog.setAction(action);
        auditLog.setEntityType(entityType);
        auditLog.setEntityId(entityId);
        auditLog.setPerformedBy(performedBy);
        auditLog.setDescription(description);
        auditLog.setIpAddress(ipAddress);
        auditLog.setStatus(status);
        
        auditLogRepository.save(auditLog);
    }

    /**
     * Registra tentativa de acesso indevido.
     */
    public void logUnauthorizedAccess(String performedBy, String ipAddress, String details) {
        logAction("UNAUTHORIZED_ACCESS", "API", 0L, performedBy, 
                 "Tentativa de acesso não autorizado", ipAddress, "UNAUTHORIZED");
    }

    /**
     * Registra falha de autenticação.
     */
    public void logFailedLogin(String email, String ipAddress) {
        logAction("FAILED_LOGIN", "User", 0L, email, 
                 "Falha na autenticação", ipAddress, "FAILED");
    }

    /**
     * Registra login bem-sucedido.
     */
    public void logSuccessfulLogin(String email, Long userId, String ipAddress) {
        logAction("LOGIN", "User", userId, email, 
                 "Login bem-sucedido", ipAddress, "SUCCESS");
    }

    /**
     * Registra criação de entidade.
     */
    public void logCreate(String entityType, Long entityId, String performedBy, 
                         String description, String ipAddress) {
        logAction("CREATE", entityType, entityId, performedBy, description, ipAddress, "SUCCESS");
    }

    /**
     * Registra atualização de entidade.
     */
    public void logUpdate(String entityType, Long entityId, String performedBy, 
                         String description, String ipAddress) {
        logAction("UPDATE", entityType, entityId, performedBy, description, ipAddress, "SUCCESS");
    }

    /**
     * Registra exclusão de entidade.
     */
    public void logDelete(String entityType, Long entityId, String performedBy, 
                         String description, String ipAddress) {
        logAction("DELETE", entityType, entityId, performedBy, description, ipAddress, "SUCCESS");
    }

    /**
     * Busca logs por ação.
     */
    public List<AuditLogModel> findByAction(String action) {
        return auditLogRepository.findByAction(action);
    }

    /**
     * Busca logs por usuário.
     */
    public List<AuditLogModel> findByPerformedBy(String performedBy) {
        return auditLogRepository.findByPerformedBy(performedBy);
    }

    /**
     * Busca logs em período.
     */
    public List<AuditLogModel> findByTimestampBetween(LocalDateTime start, LocalDateTime end) {
        return auditLogRepository.findByTimestampBetween(start, end);
    }

    /**
     * Busca logs de falhas.
     */
    public List<AuditLogModel> findFailedAttempts() {
        return auditLogRepository.findByStatus("FAILED");
    }
}
