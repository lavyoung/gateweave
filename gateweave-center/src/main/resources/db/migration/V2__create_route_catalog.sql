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
    CONSTRAINT fk_gw_route_service_operation
        FOREIGN KEY (service_operation_id)
            REFERENCES gw_service_operation (operation_id),
    CONSTRAINT ck_gw_route_invocation_timeout
        CHECK (invocation_timeout_ms > 0)
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
