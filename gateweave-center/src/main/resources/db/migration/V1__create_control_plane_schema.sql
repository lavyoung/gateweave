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

CREATE TABLE gw_gateway_group
(
    id               BIGINT       NOT NULL AUTO_INCREMENT,
    gateway_group_id VARCHAR(64)  NOT NULL,
    group_name       VARCHAR(128) NOT NULL,
    environment      VARCHAR(32)  NOT NULL,
    status           VARCHAR(32)  NOT NULL,
    revision         BIGINT       NOT NULL DEFAULT 0,
    created_at       TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at       TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    PRIMARY KEY (id),
    CONSTRAINT uk_gw_gateway_group_id UNIQUE (gateway_group_id),
    CONSTRAINT ck_gw_gateway_group_revision CHECK (revision >= 0)
);

CREATE INDEX idx_gw_gateway_group_environment_status
    ON gw_gateway_group (environment, status);

INSERT INTO gw_gateway_group
    (gateway_group_id, group_name, environment, status, revision)
VALUES
    ('default', 'Default Gateway Group', 'default', 'ENABLED', 0);

CREATE TABLE gw_route
(
    id                    BIGINT       NOT NULL AUTO_INCREMENT,
    route_id              VARCHAR(64)  NOT NULL,
    gateway_group_id      VARCHAR(64)  NOT NULL,
    path                  VARCHAR(512) NOT NULL,
    http_method           VARCHAR(16)  NOT NULL,
    service_operation_id  VARCHAR(64)  NOT NULL,
    authentication_mode   VARCHAR(32)  NOT NULL,
    invocation_timeout_ms BIGINT       NOT NULL,
    target_attributes     TEXT         NOT NULL,
    route_attributes      TEXT         NOT NULL,
    status                VARCHAR(32)  NOT NULL,
    revision              BIGINT       NOT NULL DEFAULT 0,
    created_at            TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at            TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    PRIMARY KEY (id),
    CONSTRAINT uk_gw_route_route_id UNIQUE (route_id),
    CONSTRAINT uk_gw_route_group_method_path
        UNIQUE (gateway_group_id, http_method, path),
    CONSTRAINT uk_gw_route_target
        UNIQUE (route_id, service_operation_id),
    CONSTRAINT fk_gw_route_gateway_group
        FOREIGN KEY (gateway_group_id) REFERENCES gw_gateway_group (gateway_group_id),
    CONSTRAINT fk_gw_route_service_operation
        FOREIGN KEY (service_operation_id) REFERENCES gw_service_operation (operation_id),
    CONSTRAINT ck_gw_route_invocation_timeout CHECK (invocation_timeout_ms > 0)
);

CREATE INDEX idx_gw_route_service_operation
    ON gw_route (service_operation_id);

CREATE INDEX idx_gw_route_group_status
    ON gw_route (gateway_group_id, status);

CREATE TABLE gw_route_parameter
(
    id                   BIGINT       NOT NULL AUTO_INCREMENT,
    route_id             VARCHAR(64)  NOT NULL,
    service_operation_id VARCHAR(64)  NOT NULL,
    parameter_index      INT          NOT NULL,
    parameter_name       VARCHAR(128) NOT NULL,
    created_at           TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at           TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    PRIMARY KEY (id),
    CONSTRAINT uk_gw_route_parameter_position
        UNIQUE (route_id, parameter_index),
    CONSTRAINT uk_gw_route_parameter_name
        UNIQUE (route_id, parameter_name),
    CONSTRAINT fk_gw_route_parameter_route
        FOREIGN KEY (route_id, service_operation_id)
            REFERENCES gw_route (route_id, service_operation_id),
    CONSTRAINT fk_gw_route_parameter_operation_parameter
        FOREIGN KEY (service_operation_id, parameter_index)
            REFERENCES gw_service_operation_parameter (operation_id, parameter_index)
);

CREATE INDEX idx_gw_route_parameter_operation
    ON gw_route_parameter (service_operation_id, parameter_index);

CREATE TABLE gw_gateway_node
(
    id                         BIGINT       NOT NULL AUTO_INCREMENT,
    node_id                    VARCHAR(64)  NOT NULL,
    gateway_group_id           VARCHAR(64)  NOT NULL,
    node_name                  VARCHAR(128) NOT NULL,
    advertised_address         VARCHAR(512) NOT NULL,
    status                     VARCHAR(32)  NOT NULL,
    last_heartbeat_at          TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    current_config_version     BIGINT       NOT NULL DEFAULT 0,
    last_successful_refresh_at TIMESTAMP(6) NULL,
    created_at                 TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at                 TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    PRIMARY KEY (id),
    CONSTRAINT uk_gw_gateway_node_id UNIQUE (node_id),
    CONSTRAINT uk_gw_gateway_node_group_address
        UNIQUE (gateway_group_id, advertised_address),
    CONSTRAINT fk_gw_gateway_node_group
        FOREIGN KEY (gateway_group_id) REFERENCES gw_gateway_group (gateway_group_id),
    CONSTRAINT ck_gw_gateway_node_config_version CHECK (current_config_version >= 0)
);

CREATE INDEX idx_gw_gateway_node_group_status
    ON gw_gateway_node (gateway_group_id, status);

CREATE INDEX idx_gw_gateway_node_status_heartbeat
    ON gw_gateway_node (status, last_heartbeat_at);

CREATE TABLE gw_config_release
(
    id                 BIGINT       NOT NULL AUTO_INCREMENT,
    gateway_group_id   VARCHAR(64)  NOT NULL,
    config_version     BIGINT       NOT NULL,
    checksum           VARCHAR(128) NOT NULL,
    snapshot_content   TEXT         NOT NULL,
    release_status     VARCHAR(32)  NOT NULL,
    published_at       TIMESTAMP(6) NOT NULL,
    created_at         TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    PRIMARY KEY (id),
    CONSTRAINT uk_gw_config_release_group_version
        UNIQUE (gateway_group_id, config_version),
    CONSTRAINT fk_gw_config_release_gateway_group
        FOREIGN KEY (gateway_group_id) REFERENCES gw_gateway_group (gateway_group_id),
    CONSTRAINT ck_gw_config_release_version CHECK (config_version > 0)
);

CREATE INDEX idx_gw_config_release_group_status_version
    ON gw_config_release (gateway_group_id, release_status, config_version);
