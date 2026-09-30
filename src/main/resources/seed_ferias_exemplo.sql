-- ============================================================
-- SEED DE EXEMPLO BASEADO NA FOLHA FÍSICA DE 2026
-- ============================================================

-- 1. Inserir Períodos Aquisitivos para os servidores da TI
-- 2024/2025 (Roxo: #7e22ce) e 2025/2026 (Amarelo: #eab308)
INSERT INTO `periodo_aquisitivo` (`id`, `servidor_id`, `ano_inicio`, `ano_fim`, `identificador`, `data_inicio`, `data_fim`, `limite_gozo`, `total_dias`, `dias_usados`, `dias_restantes`, `cor_hex`)
VALUES
-- Evandro (86)
(101, 86, 2024, 2025, '2024/2025', '2024-01-01', '2024-12-31', '2026-12-31', 30, 28, 2, '#7e22ce'),
(102, 86, 2025, 2026, '2025/2026', '2025-01-01', '2025-12-31', '2027-12-31', 30, 0, 30, '#eab308'),

-- Rodrigo Pausen (223)
(103, 223, 2024, 2025, '2024/2025', '2024-01-01', '2024-12-31', '2026-12-31', 30, 16, 14, '#7e22ce'),
(104, 223, 2025, 2026, '2025/2026', '2025-01-01', '2025-12-31', '2027-12-31', 30, 21, 9, '#eab308'),

-- Guilherme (110)
(105, 110, 2024, 2025, '2024/2025', '2024-01-01', '2024-12-31', '2026-12-31', 30, 18, 12, '#7e22ce'),
(106, 110, 2025, 2026, '2025/2026', '2025-01-01', '2025-12-31', '2027-12-31', 30, 19, 11, '#eab308'),

-- Livynston (149)
(107, 149, 2024, 2025, '2024/2025', '2024-01-01', '2024-12-31', '2026-12-31', 30, 24, 6, '#7e22ce'),
(108, 149, 2025, 2026, '2025/2026', '2025-01-01', '2025-12-31', '2027-12-31', 30, 18, 12, '#eab308'),

-- Jordano (125)
(109, 125, 2024, 2025, '2024/2025', '2024-01-01', '2024-12-31', '2026-12-31', 30, 0, 30, '#7e22ce'),
(110, 125, 2025, 2026, '2025/2026', '2025-01-01', '2025-12-31', '2027-12-31', 30, 12, 18, '#eab308'),

-- Vinicius (251)
(111, 251, 2024, 2025, '2024/2025', '2024-01-01', '2024-12-31', '2026-12-31', 30, 0, 30, '#7e22ce'),
(112, 251, 2025, 2026, '2025/2026', '2025-01-01', '2025-12-31', '2027-12-31', 30, 0, 30, '#eab308'),

-- Ricardo Schulz (258)
(113, 258, 2024, 2025, '2024/2025', '2024-01-01', '2024-12-31', '2026-12-31', 30, 0, 30, '#7e22ce'),
(114, 258, 2025, 2026, '2025/2026', '2025-01-01', '2025-12-31', '2027-12-31', 30, 0, 30, '#eab308')
ON DUPLICATE KEY UPDATE `total_dias` = VALUES(`total_dias`);

-- 2. Inserir Agendamentos fiéis à folha da foto
INSERT INTO `agendamento_ferias` (`id`, `servidor_id`, `periodo_aquisitivo_id`, `tipo_afastamento`, `data_inicio`, `data_fim`, `dias`, `fracao`, `status`, `alerta_conflito`, `descricao_conflito`, `observacao`)
VALUES
-- Evandro (86): Fevereiro (1 a 28)
(201, 86, 101, 'FERIAS', '2026-02-01', '2026-02-28', 28, 1, 'CONFIRMADO', FALSE, NULL, 'Férias regulares 1ª fração'),
-- Evandro (86): Maio (18 a 29)
(202, 86, 101, 'FERIAS', '2026-05-18', '2026-05-29', 12, 2, 'CONFIRMADO', FALSE, NULL, '2ª fração'),
-- Evandro (86): Novembro (9 a 27)
(203, 86, 102, 'FERIAS', '2026-11-09', '2026-11-27', 19, 1, 'CONFIRMADO', FALSE, NULL, 'Férias 2025/2026'),

-- Rodrigo Pausen (223)
(204, 223, 103, 'FERIAS', '2026-03-16', '2026-03-31', 16, 1, 'CONFIRMADO', FALSE, NULL, '1ª fração'),
(205, 223, 103, 'FERIAS', '2026-04-01', '2026-04-10', 10, 2, 'CONFIRMADO', FALSE, NULL, '2ª fração'),
(206, 223, 104, 'FERIAS', '2026-07-06', '2026-07-24', 19, 1, 'CONFIRMADO', FALSE, NULL, 'Férias de Julho'),
(207, 223, 104, 'FERIAS', '2026-09-14', '2026-09-25', 12, 2, 'CONFIRMADO', TRUE, 'Conflito com Jordano Smolark dos Santos (14/09/2026 a 25/09/2026)', 'Anotação: negado na folha por sobreposição'),
(208, 223, 104, 'FERIAS', '2026-10-01', '2026-10-09', 9, 3, 'CONFIRMADO', FALSE, NULL, 'Fração Outubro'),

-- Guilherme (110)
(209, 110, 105, 'FERIAS', '2026-04-13', '2026-04-30', 18, 1, 'CONFIRMADO', FALSE, NULL, 'Férias Abril'),
(210, 110, 106, 'FERIAS', '2026-08-10', '2026-08-28', 19, 1, 'CONFIRMADO', FALSE, NULL, 'Férias Agosto'),
(211, 110, 106, 'FERIAS', '2026-10-13', '2026-10-23', 11, 2, 'CONFIRMADO', TRUE, 'Conflito com Livynston Brouwenstyn Cavalheiro (13/10/2026 a 30/10/2026)', 'Conflito registrado na anotação manual'),

-- Livynston (149)
(212, 149, 107, 'FERIAS', '2026-04-13', '2026-04-24', 12, 1, 'CONFIRMADO', FALSE, NULL, 'Férias Abril'),
(213, 149, 107, 'FERIAS', '2026-05-04', '2026-05-15', 12, 2, 'CONFIRMADO', FALSE, NULL, 'Férias Maio'),
(214, 149, 107, 'FERIAS', '2026-06-15', '2026-06-30', 16, 3, 'CONFIRMADO', FALSE, NULL, 'Férias Junho'),
(215, 149, 108, 'FERIAS', '2026-10-13', '2026-10-30', 18, 1, 'CONFIRMADO', TRUE, 'Conflito com Guilherme Gomes Teixeira (13/10/2026 a 23/10/2026)', 'Conflito Cavalheiro'),

-- Jordano (125)
(216, 125, 110, 'FERIAS', '2026-09-14', '2026-09-25', 12, 1, 'CONFIRMADO', TRUE, 'Conflito com Rodrigo José Pausen (14/09/2026 a 25/09/2026)', 'Anotação na folha: 14 a 25/9 negado por choque com Pausen')
ON DUPLICATE KEY UPDATE `dias` = VALUES(`dias`), `descricao_conflito` = VALUES(`descricao_conflito`);
