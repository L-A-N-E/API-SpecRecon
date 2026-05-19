-- Tabela de Auditoria
CREATE TABLE IF NOT EXISTS `audit_log` (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    action VARCHAR(50) NOT NULL,
    entity_type VARCHAR(100) NOT NULL,
    entity_id BIGINT NOT NULL,
    performed_by VARCHAR(255) NOT NULL,
    description TEXT,
    ip_address VARCHAR(45) NOT NULL,
    timestamp DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    status VARCHAR(50) NOT NULL,
    details TEXT,
    
    -- Índices para buscas frequentes
    INDEX idx_action (action),
    INDEX idx_entity_type (entity_type),
    INDEX idx_performed_by (performed_by),
    INDEX idx_timestamp (timestamp),
    INDEX idx_status (status)
);

-- Tabela de Retenção de Dados (política de apagamento)
CREATE TABLE IF NOT EXISTS `data_retention_policy` (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    entity_type VARCHAR(100) NOT NULL UNIQUE,
    retention_days INT NOT NULL DEFAULT 365,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    
    CONSTRAINT chk_retention_days CHECK (retention_days > 0)
);

-- Inserir políticas de retenção padrão
INSERT INTO `data_retention_policy` (entity_type, retention_days) VALUES
('User', 365),           -- Guardar usuários por 1 ano
('Vehicle', 730),        -- Guardar veículos por 2 anos
('AuditLog', 2555),      -- Guardar logs por 7 anos (conformidade)
('Session', 30);         -- Guardar sessões por 30 dias
