-- ============================================================
-- MÓDULO DE GESTÃO DE FÉRIAS E ESCALA ANUAL
-- ============================================================

-- 1. Adicionar coluna setor na tabela servidor se não existir
SET @col_exists = 0;
SELECT COUNT(*) INTO @col_exists 
FROM information_schema.COLUMNS 
WHERE TABLE_SCHEMA = DATABASE() 
  AND TABLE_NAME = 'servidor' 
  AND COLUMN_NAME = 'setor';

SET @query = IF(@col_exists = 0, 
    'ALTER TABLE `servidor` ADD COLUMN `setor` VARCHAR(100) NULL AFTER `cargo`', 
    'SELECT 1');
PREPARE stmt FROM @query;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- 2. Tabela de Períodos Aquisitivos
CREATE TABLE IF NOT EXISTS `periodo_aquisitivo` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
    `servidor_id` INT NOT NULL,
    `ano_inicio` INT NOT NULL,
    `ano_fim` INT NOT NULL,
    `identificador` VARCHAR(30) NOT NULL,
    `data_inicio` DATE NOT NULL,
    `data_fim` DATE NOT NULL,
    `limite_gozo` DATE NULL,
    `total_dias` INT NOT NULL DEFAULT 30,
    `dias_usados` INT NOT NULL DEFAULT 0,
    `dias_restantes` INT NOT NULL DEFAULT 30,
    `cor_hex` VARCHAR(20) NOT NULL DEFAULT '#eab308',
    `criado_em` DATETIME NULL DEFAULT CURRENT_TIMESTAMP,
    `atualizado_em` DATETIME NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT `fk_periodo_servidor` FOREIGN KEY (`servidor_id`) REFERENCES `servidor`(`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 3. Tabela de Agendamentos / Afastamentos
CREATE TABLE IF NOT EXISTS `agendamento_ferias` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
    `servidor_id` INT NOT NULL,
    `periodo_aquisitivo_id` BIGINT NULL,
    `tipo_afastamento` VARCHAR(30) NOT NULL DEFAULT 'FERIAS',
    `data_inicio` DATE NOT NULL,
    `data_fim` DATE NOT NULL,
    `dias` INT NOT NULL,
    `fracao` INT NULL,
    `status` VARCHAR(20) NOT NULL DEFAULT 'CONFIRMADO',
    `alerta_conflito` BOOLEAN NOT NULL DEFAULT FALSE,
    `descricao_conflito` TEXT NULL,
    `observacao` TEXT NULL,
    `criado_em` DATETIME NULL DEFAULT CURRENT_TIMESTAMP,
    `atualizado_em` DATETIME NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT `fk_agendamento_servidor` FOREIGN KEY (`servidor_id`) REFERENCES `servidor`(`id`) ON DELETE CASCADE,
    CONSTRAINT `fk_agendamento_periodo` FOREIGN KEY (`periodo_aquisitivo_id`) REFERENCES `periodo_aquisitivo`(`id`) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 4. Tabela de Feriados
CREATE TABLE IF NOT EXISTS `feriado` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
    `data` DATE NOT NULL,
    `descricao` VARCHAR(100) NOT NULL,
    `tipo` VARCHAR(30) NOT NULL DEFAULT 'NACIONAL',
    UNIQUE KEY `uk_feriado_data` (`data`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 5. Carga de Feriados Nacionais 2025, 2026 e 2027
INSERT IGNORE INTO `feriado` (`data`, `descricao`, `tipo`) VALUES
-- 2025
('2025-01-01', 'Confraternização Universal', 'NACIONAL'),
('2025-03-03', 'Carnaval', 'FACULTATIVO'),
('2025-03-04', 'Carnaval', 'FACULTATIVO'),
('2025-04-18', 'Sexta-feira Santa', 'NACIONAL'),
('2025-04-21', 'Tiradentes', 'NACIONAL'),
('2025-05-01', 'Dia do Trabalho', 'NACIONAL'),
('2025-06-19', 'Corpus Christi', 'FACULTATIVO'),
('2025-09-07', 'Independência do Brasil', 'NACIONAL'),
('2025-10-12', 'Nossa Senhora Aparecida', 'NACIONAL'),
('2025-11-02', 'Finados', 'NACIONAL'),
('2025-11-15', 'Proclamação da República', 'NACIONAL'),
('2025-11-20', 'Dia da Consciência Negra', 'NACIONAL'),
('2025-12-25', 'Natal', 'NACIONAL'),
-- 2026
('2026-01-01', 'Confraternização Universal', 'NACIONAL'),
('2026-02-16', 'Carnaval', 'FACULTATIVO'),
('2026-02-17', 'Carnaval', 'FACULTATIVO'),
('2026-04-03', 'Sexta-feira Santa', 'NACIONAL'),
('2026-04-21', 'Tiradentes', 'NACIONAL'),
('2026-05-01', 'Dia do Trabalho', 'NACIONAL'),
('2026-06-04', 'Corpus Christi', 'FACULTATIVO'),
('2026-09-07', 'Independência do Brasil', 'NACIONAL'),
('2026-10-12', 'Nossa Senhora Aparecida', 'NACIONAL'),
('2026-11-02', 'Finados', 'NACIONAL'),
('2026-11-15', 'Proclamação da República', 'NACIONAL'),
('2026-11-20', 'Dia da Consciência Negra', 'NACIONAL'),
('2026-12-25', 'Natal', 'NACIONAL'),
-- 2027
('2027-01-01', 'Confraternização Universal', 'NACIONAL'),
('2027-02-08', 'Carnaval', 'FACULTATIVO'),
('2027-02-09', 'Carnaval', 'FACULTATIVO'),
('2027-03-26', 'Sexta-feira Santa', 'NACIONAL'),
('2027-04-21', 'Tiradentes', 'NACIONAL'),
('2027-05-01', 'Dia do Trabalho', 'NACIONAL'),
('2027-05-27', 'Corpus Christi', 'FACULTATIVO'),
('2027-09-07', 'Independência do Brasil', 'NACIONAL'),
('2027-10-12', 'Nossa Senhora Aparecida', 'NACIONAL'),
('2027-11-02', 'Finados', 'NACIONAL'),
('2027-11-15', 'Proclamação da República', 'NACIONAL'),
('2027-11-20', 'Dia da Consciência Negra', 'NACIONAL'),
('2027-12-25', 'Natal', 'NACIONAL');

-- 6. Configuração dos servidores da foto no Setor de Informática / Secretaria de Administração
UPDATE `servidor` 
SET `secretaria_id` = 2, `setor` = 'Setor de Informática'
WHERE `id` IN (86, 110, 125, 149, 223, 251);

-- 7. Criar Ricardo Schulz se não existir
INSERT INTO `servidor` (`nome`, `cargo`, `setor`, `matricula`, `email`, `telefone`, `secretaria_id`, `ativo_id`)
SELECT 'Ricardo Schulz', 'Servidor Municipal', 'Setor de Informática', 25614, 'ricardo.schulz@prefeitura.gov.br', '(51) 99999-9999', 2, 1
WHERE NOT EXISTS (SELECT 1 FROM `servidor` WHERE `matricula` = 25614);
