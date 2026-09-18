-- ============================================================
-- SCRIPT DE ATUALIZACAO DOS LOTES CONTRATUAIS E EMPENHOS 2026
-- Outsourcing de Impressao - Prefeitura Municipal
-- Nao altera tabelas existentes do sistema (contrato, secretaria, servidor)
-- ============================================================

-- 1. ATUALIZAR / INSERIR OS 5 LOTES OFICIAIS DO CONTRATO
INSERT INTO `lote_impressao` (`id`, `numero_lote`, `descricao`, `tipo`, `franquia_mono`, `franquia_color`, `valor_locacao_mensal`, `valor_excedente_mono`, `valor_excedente_color`, `ativo`)
VALUES
(1, 1, 'Multifuncional Laser Monocromática (Pequeno/Médio Porte)', 'MONO', 1000, 0, 30.00, 0.0300, 0.0000, TRUE),
(2, 2, 'Multifuncional Laser Monocromática (Médio/Grande Porte - SMED)', 'MONO', 5000, 0, 201.00, 0.0700, 0.0000, TRUE),
(3, 3, 'Multifuncional Laser Policromática Híbrida (Colorida)', 'COLOR', 1500, 500, 204.00, 0.0400, 0.2900, TRUE),
(4, 4, 'Impressora Laser Monocromática Porte Pequeno/Médio (Simples PB)', 'MONO', 1000, 0, 26.00, 0.0200, 0.0000, TRUE),
(5, 5, 'Impressora Laser Colorida Especial (500 páginas)', 'COLOR', 0, 500, 204.00, 0.0400, 0.2900, TRUE)
ON DUPLICATE KEY UPDATE
  `numero_lote` = VALUES(`numero_lote`),
  `descricao` = VALUES(`descricao`),
  `tipo` = VALUES(`tipo`),
  `franquia_mono` = VALUES(`franquia_mono`),
  `franquia_color` = VALUES(`franquia_color`),
  `valor_locacao_mensal` = VALUES(`valor_locacao_mensal`),
  `valor_excedente_mono` = VALUES(`valor_excedente_mono`),
  `valor_excedente_color` = VALUES(`valor_excedente_color`),
  `ativo` = TRUE;

-- 2. ATUALIZAR DOTACAO ORCAMENTARIA DOS EMPENHOS (CONFORME PLANILHA MUNICIPAL 2026)
UPDATE `empenho_impressao` SET `valor_total` = 36014.00, `saldo` = 21908.92 WHERE `numero_empenho` = '2625';
UPDATE `empenho_impressao` SET `valor_total` = 11484.00, `saldo` = 6608.86 WHERE `numero_empenho` = '2516';
UPDATE `empenho_impressao` SET `valor_total` = 8723.00, `saldo` = 5861.29 WHERE `numero_empenho` = '2517';
UPDATE `empenho_impressao` SET `valor_total` = 990.00, `saldo` = 552.09 WHERE `numero_empenho` = '2518';
UPDATE `empenho_impressao` SET `valor_total` = 495.00, `saldo` = 322.41 WHERE `numero_empenho` = '2519';
UPDATE `empenho_impressao` SET `valor_total` = 1320.00, `saldo` = 766.59 WHERE `numero_empenho` = '2520';
UPDATE `empenho_impressao` SET `valor_total` = 15741.00, `saldo` = 8781.75 WHERE `numero_empenho` = '2522';
UPDATE `empenho_impressao` SET `valor_total` = 156794.00, `saldo` = 65344.20 WHERE `numero_empenho` = '2521';
UPDATE `empenho_impressao` SET `valor_total` = 50000.00, `saldo` = 50000.00 WHERE `numero_empenho` = '3000';

-- 3. VINCULAR AS IMPRESSORAS DA SMED DO LOTE 02 (MEDIO/GRANDE R$ 201,00) AO LOTE_ID 2
UPDATE `impressora` SET `lote_id` = (SELECT `id` FROM `lote_impressao` WHERE `numero_lote` = 2 LIMIT 1) 
WHERE `item_pedido` IN (51, 53, 54, 56, 57, 59, 60, 62, 63, 64, 65, 68, 70, 71, 73, 74, 85);

-- 4. VINCULAR IMPRESSORAS DO LOTE 03 (COLORIDA HIBRIDA R$ 204,00) AO LOTE_ID 3
UPDATE `impressora` SET `lote_id` = (SELECT `id` FROM `lote_impressao` WHERE `numero_lote` = 3 LIMIT 1) 
WHERE `item_pedido` IN (1, 10, 20, 24, 30, 31, 32, 45, 52, 55, 58, 61, 66, 67, 69, 72, 75, 76, 77, 78, 79, 80, 81, 82, 96);

-- 5. VINCULAR IMPRESSORA DO LOTE 05 AO LOTE_ID 5
UPDATE `impressora` SET `lote_id` = (SELECT `id` FROM `lote_impressao` WHERE `numero_lote` = 5 LIMIT 1) 
WHERE `item_pedido` = 501;
