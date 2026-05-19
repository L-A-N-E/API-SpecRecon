package br.com.lane.SpecRecon.service;

import br.com.lane.SpecRecon.repository.AuditLogRepository;
import br.com.lane.SpecRecon.repository.DataRetentionPolicyRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * Job de retenção e descarte seguro de dados (Eixo 4).
 *
 * Lê data_retention_policy e remove/anonimiza registros que excederam
 * o tempo de retenção. Roda 1x por dia às 03:00.
 */
@Service
public class DataRetentionService {

    private static final Logger logger = LoggerFactory.getLogger(DataRetentionService.class);

    private final DataRetentionPolicyRepository policyRepository;
    private final AuditLogRepository auditLogRepository;

    public DataRetentionService(DataRetentionPolicyRepository policyRepository,
                                AuditLogRepository auditLogRepository) {
        this.policyRepository = policyRepository;
        this.auditLogRepository = auditLogRepository;
    }

    /**
     * Executa diariamente às 03:00 (horário de baixo tráfego).
     * Cron: segundo minuto hora dia mês dia-da-semana
     */
    @Scheduled(cron = "0 0 3 * * *")
    @Transactional
    public void enforceRetentionPolicies() {
        logger.info("Iniciando job de retenção de dados...");

        List<Map<String, Object>> policies = policyRepository.findAll();

        for (Map<String, Object> policy : policies) {
            String entityType = (String) policy.get("entity_type");
            Integer retentionDays = (Integer) policy.get("retention_days");

            LocalDateTime cutoff = LocalDateTime.now().minusDays(retentionDays);

            try {
                int removed = applyPolicy(entityType, cutoff);
                logger.info("Retenção [{}]: {} registros mais antigos que {} dias removidos/anonimizados",
                        entityType, removed, retentionDays);
            } catch (Exception e) {
                logger.error("Falha ao aplicar política de retenção para {}: {}", entityType, e.getMessage());
            }
        }

        logger.info("Job de retenção de dados concluído.");
    }

    private int applyPolicy(String entityType, LocalDateTime cutoff) {
        return switch (entityType) {
            case "AuditLog" -> auditLogRepository.deleteByTimestampBefore(cutoff);
            // Adicione outras entidades aqui conforme política
            default -> 0;
        };
    }
}