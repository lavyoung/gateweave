CREATE TABLE gw_service
(
    id               BIGINT       NOT NULL AUTO_INCREMENT,
    service_id       VARCHAR(64)  NOT NULL,
    application_name VARCHAR(128) NOT NULL,
    protocol         VARCHAR(32)  NOT NULL,
    namespace        VARCHAR(128) NOT NULL,
    service_version  VARCHAR(64)  NOT NULL,
    status           VARCHAR(32)  NOT NULL,
    revision         BIGINT       NOT NULL DEFAULT 0,
    created_at       TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at       TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    PRIMARY KEY (id),
    CONSTRAINT uk_gw_service_service_id UNIQUE (service_id)
);

CREATE INDEX idx_gw_service_application
    ON gw_service (application_name);

CREATE TABLE gw_service_operation
(
    id             BIGINT       NOT NULL AUTO_INCREMENT,
    operation_id   VARCHAR(64)  NOT NULL,
    service_id     VARCHAR(64)  NOT NULL,
    interface_name VARCHAR(256) NOT NULL,
    operation_name VARCHAR(128) NOT NULL,
    status         VARCHAR(32)  NOT NULL,
    revision       BIGINT       NOT NULL DEFAULT 0,
    created_at     TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at     TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    PRIMARY KEY (id),
    CONSTRAINT uk_gw_service_operation_id UNIQUE (operation_id),
    CONSTRAINT uk_gw_service_operation_definition
        UNIQUE (service_id, interface_name, operation_name),
    CONSTRAINT fk_gw_service_operation_service
        FOREIGN KEY (service_id) REFERENCES gw_service (service_id)
);

CREATE INDEX idx_gw_service_operation_service
    ON gw_service_operation (service_id);

CREATE TABLE gw_service_operation_parameter
(
    id              BIGINT       NOT NULL AUTO_INCREMENT,
    operation_id    VARCHAR(64)  NOT NULL,
    parameter_index INT          NOT NULL,
    parameter_name  VARCHAR(128) NOT NULL,
    type_name       VARCHAR(256) NOT NULL,
    created_at      TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at      TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    PRIMARY KEY (id),
    CONSTRAINT uk_gw_operation_parameter_position
        UNIQUE (operation_id, parameter_index),
    CONSTRAINT uk_gw_operation_parameter_name
        UNIQUE (operation_id, parameter_name),
    CONSTRAINT fk_gw_operation_parameter_operation
        FOREIGN KEY (operation_id) REFERENCES gw_service_operation (operation_id)
);

CREATE INDEX idx_gw_operation_parameter_operation
    ON gw_service_operation_parameter (operation_id);
