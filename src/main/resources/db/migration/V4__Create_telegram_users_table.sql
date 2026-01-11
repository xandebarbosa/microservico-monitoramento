CREATE TABLE usuarios_telegram (
    id BIGSERIAL PRIMARY KEY,
    telegram_id VARCHAR(50) NOT NULL UNIQUE,
    username VARCHAR(100),
    primeiro_nome VARCHAR(100),
    sobrenome VARCHAR(100),
    data_cadastro TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    ultimo_acesso TIMESTAMP
);