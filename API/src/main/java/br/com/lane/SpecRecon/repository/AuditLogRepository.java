package br.com.lane.SpecRecon.repository;

import br.com.lane.SpecRecon.model.AuditLogModel;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Repository para AuditLog.
 */
@Repository
public interface AuditLogRepository extends JpaRepository<AuditLogModel, Long> {

    // Buscar logs por ação
    List<AuditLogModel> findByAction(String action);

    // Buscar logs por tipo de entidade
    List<AuditLogModel> findByEntityType(String entityType);

    // Buscar logs por usuário
    List<AuditLogModel> findByPerformedBy(String performedBy);

    // Buscar logs em período
    List<AuditLogModel> findByTimestampBetween(LocalDateTime start, LocalDateTime end);

    // Buscar logs de acesso não autorizado
    List<AuditLogModel> findByStatus(String status);

    // Eixo 4 - Retenção: deletar logs mais antigos que o cutoff
    @Modifying
    @Query("DELETE FROM AuditLogModel a WHERE a.timestamp < :cutoff")
    int deleteByTimestampBefore(@Param("cutoff") LocalDateTime cutoff);
}