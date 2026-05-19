package br.com.lane.SpecRecon.repository;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Map;

/**
 * Repositório simples (JdbcTemplate) para a tabela data_retention_policy.
 * Não criamos entity JPA pois a tabela é de configuração, não de domínio.
 */
@Repository
public class DataRetentionPolicyRepository {

    private final JdbcTemplate jdbc;

    public DataRetentionPolicyRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public List<Map<String, Object>> findAll() {
        return jdbc.queryForList("SELECT entity_type, retention_days FROM data_retention_policy");
    }
}