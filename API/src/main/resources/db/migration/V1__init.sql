-- UNIT
CREATE TABLE IF NOT EXISTS `unit` (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    name VARCHAR(255) NOT NULL,
    symbol VARCHAR(64) NOT NULL UNIQUE,
    dimension VARCHAR(32) NOT NULL,
    conversion_factor_to_base DECIMAL(19,6) NOT NULL
);

-- SPECIFICATION TYPE
CREATE TABLE IF NOT EXISTS `specification_type` (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    name VARCHAR(255) NOT NULL UNIQUE,
    category VARCHAR(255) NOT NULL,
    description VARCHAR(1024) NOT NULL,
    data_type VARCHAR(32) NOT NULL,
    default_unit_id BIGINT,

    CONSTRAINT fk_spec_type_default_unit
    FOREIGN KEY (default_unit_id)
    REFERENCES `unit`(id)
    ON DELETE SET NULL
    ON UPDATE CASCADE
);

-- VEHICLE
CREATE TABLE IF NOT EXISTS `vehicle` (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    brand VARCHAR(255) NOT NULL,
    model VARCHAR(255) NOT NULL,
    version VARCHAR(255) NOT NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- USER
CREATE TABLE IF NOT EXISTS `user` (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    email VARCHAR(512) NOT NULL UNIQUE,
    password VARCHAR(255) NOT NULL,
    role VARCHAR(32) NOT NULL
);

-- VEHICLE SPECIFICATION
CREATE TABLE IF NOT EXISTS `vehicle_specification` (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    vehicle_id BIGINT NOT NULL,
    specification_type_id BIGINT NOT NULL,
    unit_id BIGINT,

    value_numeric DECIMAL(19,6),
    value_text VARCHAR(1024),
    is_available BOOLEAN NOT NULL DEFAULT TRUE,

    -- Garante consistência de valor
    CONSTRAINT chk_value
    CHECK (
        (value_numeric IS NOT NULL AND value_text IS NULL)
            OR
        (value_numeric IS NULL AND value_text IS NOT NULL)
    ),

    -- Evita duplicidade por spec
    CONSTRAINT uk_vehicle_spec UNIQUE (vehicle_id, specification_type_id),

    CONSTRAINT fk_vehicle_spec_vehicle
    FOREIGN KEY (vehicle_id)
    REFERENCES `vehicle`(id)
    ON DELETE CASCADE,

    CONSTRAINT fk_vehicle_spec_type
    FOREIGN KEY (specification_type_id)
    REFERENCES `specification_type`(id)
    ON DELETE CASCADE,

    CONSTRAINT fk_vehicle_spec_unit
    FOREIGN KEY (unit_id)
    REFERENCES `unit`(id)
    ON DELETE SET NULL
);

-- INDEXES
CREATE INDEX idx_vehicle_spec_vehicle
    ON `vehicle_specification`(vehicle_id);

CREATE INDEX idx_vehicle_spec_type
    ON `vehicle_specification`(specification_type_id);
