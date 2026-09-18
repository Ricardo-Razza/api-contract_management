-- ============================================================
-- MODULO DE GESTAO DE IMPRESSORAS (OUTSOURCING DE IMPRESSAO)
-- Estrutura de tabelas baseada no diagrama conceitual e na planilha de controle 2026.
-- As tabelas existentes (contrato, secretaria, etc.) NAO sao modificadas.
-- ============================================================

CREATE TABLE IF NOT EXISTS `lote_impressao` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
    `numero_lote` INT NOT NULL,
    `descricao` VARCHAR(255) NOT NULL,
    `tipo` VARCHAR(50) NOT NULL,
    `franquia_mono` INT NOT NULL DEFAULT 0,
    `franquia_color` INT NOT NULL DEFAULT 0,
    `valor_locacao_mensal` DECIMAL(10, 2) NOT NULL DEFAULT 0.00,
    `valor_excedente_mono` DECIMAL(10, 4) NOT NULL DEFAULT 0.0000,
    `valor_excedente_color` DECIMAL(10, 4) NOT NULL DEFAULT 0.0000,
    `contrato_id` BIGINT NULL,
    `ativo` BOOLEAN NOT NULL DEFAULT TRUE,
    CONSTRAINT `fk_lote_contrato` FOREIGN KEY (`contrato_id`) REFERENCES `contrato`(`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS `empenho_impressao` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
    `numero_empenho` VARCHAR(50) NOT NULL,
    `ano` INT NOT NULL,
    `secretaria_id` BIGINT NOT NULL,
    `descricao` VARCHAR(255) NULL,
    `valor_total` DECIMAL(12, 2) NOT NULL DEFAULT 0.00,
    `saldo` DECIMAL(12, 2) NOT NULL DEFAULT 0.00,
    `ativo` BOOLEAN NOT NULL DEFAULT TRUE,
    CONSTRAINT `fk_empenho_secretaria` FOREIGN KEY (`secretaria_id`) REFERENCES `secretaria`(`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS `impressora` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
    `item_pedido` INT NULL,
    `numero_serie` VARCHAR(100) NULL,
    `fabricante` VARCHAR(100) NOT NULL,
    `modelo` VARCHAR(100) NOT NULL,
    `tipo_impressao` VARCHAR(30) NOT NULL DEFAULT 'MONO',
    `lote_id` BIGINT NULL,
    `ip` VARCHAR(45) NULL,
    `ativo` BOOLEAN NOT NULL DEFAULT TRUE,
    `criado_em` DATETIME NULL,
    `atualizado_em` DATETIME NULL,
    CONSTRAINT `fk_impressora_lote` FOREIGN KEY (`lote_id`) REFERENCES `lote_impressao`(`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS `instalacao_impressora` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
    `impressora_id` BIGINT NOT NULL,
    `secretaria_id` BIGINT NOT NULL,
    `empenho_id` BIGINT NULL,
    `local_instalacao` VARCHAR(255) NOT NULL,
    `endereco` VARCHAR(255) NULL,
    `responsavel` VARCHAR(150) NULL,
    `transformador` VARCHAR(50) NULL,
    `data_instalacao` DATE NOT NULL,
    `contador_instalacao_mono` INT NOT NULL DEFAULT 0,
    `contador_instalacao_color` INT NOT NULL DEFAULT 0,
    `data_retirada` DATE NULL,
    `contador_retirada_mono` INT NULL,
    `contador_retirada_color` INT NULL,
    `motivo_retirada` VARCHAR(255) NULL,
    `status` VARCHAR(30) NOT NULL DEFAULT 'ATIVA',
    CONSTRAINT `fk_instalacao_impressora` FOREIGN KEY (`impressora_id`) REFERENCES `impressora`(`id`),
    CONSTRAINT `fk_instalacao_secretaria` FOREIGN KEY (`secretaria_id`) REFERENCES `secretaria`(`id`),
    CONSTRAINT `fk_instalacao_empenho` FOREIGN KEY (`empenho_id`) REFERENCES `empenho_impressao`(`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS `leitura_contador` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
    `impressora_id` BIGINT NOT NULL,
    `instalacao_id` BIGINT NULL,
    `mes_referencia` INT NOT NULL,
    `ano_referencia` INT NOT NULL,
    `data_leitura` DATE NOT NULL,
    `leitura_mono_anterior` INT NOT NULL DEFAULT 0,
    `leitura_mono_atual` INT NOT NULL DEFAULT 0,
    `copias_mono` INT NOT NULL DEFAULT 0,
    `leitura_color_anterior` INT NOT NULL DEFAULT 0,
    `leitura_color_atual` INT NOT NULL DEFAULT 0,
    `copias_color` INT NOT NULL DEFAULT 0,
    `proporcao` DECIMAL(5, 2) NOT NULL DEFAULT 1.00,
    `franquia_mono_aplicada` INT NOT NULL DEFAULT 0,
    `franquia_color_aplicada` INT NOT NULL DEFAULT 0,
    `excedente_mono` INT NOT NULL DEFAULT 0,
    `excedente_color` INT NOT NULL DEFAULT 0,
    `valor_locacao` DECIMAL(10, 2) NOT NULL DEFAULT 0.00,
    `valor_excedente_mono` DECIMAL(10, 2) NOT NULL DEFAULT 0.00,
    `valor_excedente_color` DECIMAL(10, 2) NOT NULL DEFAULT 0.00,
    `valor_total` DECIMAL(10, 2) NOT NULL DEFAULT 0.00,
    `origem_leitura` VARCHAR(30) NOT NULL DEFAULT 'MANUAL',
    `observacoes` VARCHAR(255) NULL,
    CONSTRAINT `fk_leitura_impressora` FOREIGN KEY (`impressora_id`) REFERENCES `impressora`(`id`),
    CONSTRAINT `fk_leitura_instalacao` FOREIGN KEY (`instalacao_id`) REFERENCES `instalacao_impressora`(`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS `faturamento_mensal` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
    `mes_referencia` INT NOT NULL,
    `ano_referencia` INT NOT NULL,
    `data_fechamento` DATE NOT NULL,
    `total_equipamentos` INT NOT NULL DEFAULT 0,
    `total_copias_mono` INT NOT NULL DEFAULT 0,
    `total_copias_color` INT NOT NULL DEFAULT 0,
    `total_excedente_mono` INT NOT NULL DEFAULT 0,
    `total_excedente_color` INT NOT NULL DEFAULT 0,
    `valor_total_locacao` DECIMAL(12, 2) NOT NULL DEFAULT 0.00,
    `valor_total_excedentes` DECIMAL(12, 2) NOT NULL DEFAULT 0.00,
    `valor_total_fatura` DECIMAL(12, 2) NOT NULL DEFAULT 0.00,
    `status` VARCHAR(30) NOT NULL DEFAULT 'ABERTO'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ============================================================
-- SEED DOS LOTES CONTRATUAIS DA PREFEITURA
-- ============================================================
INSERT IGNORE INTO `lote_impressao` (`id`, `numero_lote`, `descricao`, `tipo`, `franquia_mono`, `franquia_color`, `valor_locacao_mensal`, `valor_excedente_mono`, `valor_excedente_color`, `ativo`)
VALUES
(1, 1, 'Multifuncional Laser Monocromática (Pequeno/Médio Porte)', 'MONO', 1000, 0, 30.00, 0.0300, 0.0000, TRUE),
(2, 3, 'Multifuncional Laser Policromática Híbrida (Colorida)', 'COLOR', 1500, 500, 204.00, 0.0400, 0.2900, TRUE),
(3, 4, 'Impressora Laser Monocromática Porte Pequeno/Médio', 'MONO', 1000, 0, 26.00, 0.0200, 0.0000, TRUE);
