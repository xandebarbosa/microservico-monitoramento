-- V1__Create_monitoramento_tables.sql

CREATE TABLE placas_monitoradas (
    id BIGSERIAL PRIMARY KEY,
    placa VARCHAR(7) NOT NULL UNIQUE,
    marca_modelo VARCHAR(255),
    cor VARCHAR(255),
    motivo VARCHAR(255),
    status_ativo BOOLEAN NOT NULL,
    observacao VARCHAR(1000),
    interessado VARCHAR(255),
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL
);

CREATE TABLE alertas_passagens (
    id BIGSERIAL PRIMARY KEY,
    concessionaria VARCHAR(255),
    data DATE,
    hora TIME,
    placa VARCHAR(255),
    praca VARCHAR(255),
    rodovia VARCHAR(255),
    km VARCHAR(255),
    sentido VARCHAR(255),
    timestamp_alerta TIMESTAMP NOT NULL,
    placa_monitorada_id BIGINT NOT NULL,
    CONSTRAINT fk_placa_monitorada
        FOREIGN KEY(placa_monitorada_id)
        REFERENCES placas_monitoradas(id)
        ON DELETE CASCADE
);