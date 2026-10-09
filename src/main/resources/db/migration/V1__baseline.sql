SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- --------------------------------------------------
-- Table structure for agendamento_ferias
-- --------------------------------------------------
CREATE TABLE `agendamento_ferias` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `servidor_id` int NOT NULL,
  `periodo_aquisitivo_id` bigint DEFAULT NULL,
  `tipo_afastamento` varchar(30) COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'FERIAS',
  `data_inicio` date NOT NULL,
  `data_fim` date NOT NULL,
  `dias` int NOT NULL,
  `fracao` int DEFAULT NULL,
  `status` varchar(20) COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'CONFIRMADO',
  `alerta_conflito` tinyint(1) NOT NULL DEFAULT '0',
  `descricao_conflito` text COLLATE utf8mb4_unicode_ci,
  `observacao` text COLLATE utf8mb4_unicode_ci,
  `criado_em` datetime DEFAULT CURRENT_TIMESTAMP,
  `atualizado_em` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `fk_agendamento_servidor` (`servidor_id`),
  KEY `fk_agendamento_periodo` (`periodo_aquisitivo_id`),
  CONSTRAINT `fk_agendamento_periodo` FOREIGN KEY (`periodo_aquisitivo_id`) REFERENCES `periodo_aquisitivo` (`id`) ON DELETE SET NULL,
  CONSTRAINT `fk_agendamento_servidor` FOREIGN KEY (`servidor_id`) REFERENCES `servidor` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- --------------------------------------------------
-- Table structure for ata_registro_preco
-- --------------------------------------------------
CREATE TABLE `ata_registro_preco` (
  `id` int NOT NULL AUTO_INCREMENT,
  `numero` int NOT NULL,
  `ano` int NOT NULL,
  `data_inicio` date NOT NULL,
  `data_fim` date DEFAULT NULL,
  `tipo_id` int NOT NULL,
  `objeto` text NOT NULL,
  `ativo_id` int NOT NULL,
  `criado_em` timestamp NULL DEFAULT CURRENT_TIMESTAMP,
  `atualizado_em` timestamp NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `observacao` varchar(255) DEFAULT NULL,
  `portaria_designacao` varchar(100) DEFAULT NULL,
  `data_designacao` date DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_numero_ano` (`numero`,`ano`),
  KEY `tipo_id` (`tipo_id`),
  KEY `ativo_id` (`ativo_id`),
  CONSTRAINT `ata_registro_preco_ibfk_1` FOREIGN KEY (`tipo_id`) REFERENCES `tipo` (`id`) ON DELETE RESTRICT,
  CONSTRAINT `ata_registro_preco_ibfk_2` FOREIGN KEY (`ativo_id`) REFERENCES `ativo` (`id`) ON DELETE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- --------------------------------------------------
-- Table structure for ata_secretaria
-- --------------------------------------------------
CREATE TABLE `ata_secretaria` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `ata_id` int NOT NULL,
  `secretaria_id` int NOT NULL,
  `ativo_id` int NOT NULL,
  `criado_em` timestamp NULL DEFAULT CURRENT_TIMESTAMP,
  `atualizado_em` timestamp NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_ata_secretaria` (`ata_id`,`secretaria_id`),
  KEY `secretaria_id` (`secretaria_id`),
  KEY `ativo_id` (`ativo_id`),
  CONSTRAINT `ata_secretaria_ibfk_1` FOREIGN KEY (`ata_id`) REFERENCES `ata_registro_preco` (`id`) ON DELETE CASCADE,
  CONSTRAINT `ata_secretaria_ibfk_2` FOREIGN KEY (`secretaria_id`) REFERENCES `secretaria` (`id`) ON DELETE RESTRICT,
  CONSTRAINT `ata_secretaria_ibfk_3` FOREIGN KEY (`ativo_id`) REFERENCES `ativo` (`id`) ON DELETE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- --------------------------------------------------
-- Table structure for ativo
-- --------------------------------------------------
CREATE TABLE `ativo` (
  `id` int NOT NULL AUTO_INCREMENT,
  `situacao` varchar(255) NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `situacao` (`situacao`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- --------------------------------------------------
-- Table structure for coleta_contador_item
-- --------------------------------------------------
CREATE TABLE `coleta_contador_item` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `sessao_id` bigint NOT NULL,
  `impressora_id` bigint DEFAULT NULL,
  `item_pedido` int DEFAULT NULL,
  `ip` varchar(45) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `modelo` varchar(100) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `secretaria_sigla` varchar(50) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `local_instalacao` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `status` varchar(50) COLLATE utf8mb4_unicode_ci NOT NULL,
  `mensagem` text COLLATE utf8mb4_unicode_ci,
  `nome_arquivo` varchar(150) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `caminho_arquivo` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `contador_total` int DEFAULT NULL,
  `contador_mono` int DEFAULT NULL,
  `contador_color` int DEFAULT NULL,
  `copias_print` int DEFAULT NULL,
  `copias_copiador` int DEFAULT NULL,
  `copias_scanner` int DEFAULT NULL,
  `data_coleta` datetime DEFAULT NULL,
  `nivel_toner` int DEFAULT NULL,
  `numero_serie` varchar(100) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `metodo_coleta` varchar(30) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `data_inicio_coleta` datetime(6) DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `fk_coleta_item_sessao` (`sessao_id`),
  CONSTRAINT `fk_coleta_item_sessao` FOREIGN KEY (`sessao_id`) REFERENCES `coleta_contador_sessao` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- --------------------------------------------------
-- Table structure for coleta_contador_sessao
-- --------------------------------------------------
CREATE TABLE `coleta_contador_sessao` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `ano_referencia` int NOT NULL,
  `mes_referencia` int NOT NULL,
  `data_inicio` datetime NOT NULL,
  `data_fim` datetime DEFAULT NULL,
  `status` varchar(50) COLLATE utf8mb4_unicode_ci NOT NULL,
  `total_impressoras` int NOT NULL DEFAULT '0',
  `total_sucesso` int NOT NULL DEFAULT '0',
  `total_falhas` int NOT NULL DEFAULT '0',
  `diretorio_prints` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- --------------------------------------------------
-- Table structure for contrato
-- --------------------------------------------------
CREATE TABLE `contrato` (
  `id` int NOT NULL AUTO_INCREMENT,
  `numero` int NOT NULL,
  `ano` int NOT NULL,
  `data_inicio` date NOT NULL,
  `data_fim` date DEFAULT NULL,
  `tipo_id` int NOT NULL,
  `objeto` text NOT NULL,
  `nome_contratado` varchar(255) NOT NULL,
  `portaria_designacao` varchar(100) DEFAULT NULL,
  `data_designacao` date DEFAULT NULL,
  `ativo_id` int NOT NULL,
  `criado_em` timestamp NULL DEFAULT CURRENT_TIMESTAMP,
  `atualizado_em` timestamp NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `observacao` varchar(255) DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_contrato_numero_ano` (`numero`,`ano`),
  KEY `idx_contrato_tipo` (`tipo_id`),
  KEY `idx_contrato_ativo` (`ativo_id`),
  CONSTRAINT `fk_contrato_ativo` FOREIGN KEY (`ativo_id`) REFERENCES `ativo` (`id`),
  CONSTRAINT `fk_contrato_tipo` FOREIGN KEY (`tipo_id`) REFERENCES `tipo` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- --------------------------------------------------
-- Table structure for contrato_secretaria
-- --------------------------------------------------
CREATE TABLE `contrato_secretaria` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `contrato_id` int NOT NULL,
  `secretaria_id` int NOT NULL,
  `ativo_id` int NOT NULL,
  `criado_em` timestamp NULL DEFAULT CURRENT_TIMESTAMP,
  `atualizado_em` timestamp NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_cs_contrato` (`contrato_id`),
  KEY `idx_cs_secretaria` (`secretaria_id`),
  KEY `idx_cs_ativo` (`ativo_id`),
  CONSTRAINT `fk_cs_ativo` FOREIGN KEY (`ativo_id`) REFERENCES `ativo` (`id`),
  CONSTRAINT `fk_cs_contrato` FOREIGN KEY (`contrato_id`) REFERENCES `contrato` (`id`),
  CONSTRAINT `fk_cs_secretaria` FOREIGN KEY (`secretaria_id`) REFERENCES `secretaria` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- --------------------------------------------------
-- Table structure for documento_anexo
-- --------------------------------------------------
CREATE TABLE `documento_anexo` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `nome_original` varchar(255) COLLATE utf8mb4_unicode_ci NOT NULL,
  `nome_arquivo` varchar(255) COLLATE utf8mb4_unicode_ci NOT NULL,
  `tipo_documento` varchar(50) COLLATE utf8mb4_unicode_ci NOT NULL,
  `content_type` varchar(100) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `tamanho_bytes` bigint DEFAULT NULL,
  `descricao` varchar(500) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `contrato_id` bigint DEFAULT NULL,
  `ata_id` bigint DEFAULT NULL,
  `criado_em` datetime DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_doc_contrato` (`contrato_id`),
  KEY `idx_doc_ata` (`ata_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- --------------------------------------------------
-- Table structure for empenho_impressao
-- --------------------------------------------------
CREATE TABLE `empenho_impressao` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `ano` int NOT NULL,
  `ativo` bit(1) NOT NULL,
  `descricao` varchar(255) DEFAULT NULL,
  `numero_empenho` varchar(50) NOT NULL,
  `saldo` decimal(12,2) NOT NULL,
  `valor_total` decimal(12,2) NOT NULL,
  `secretaria_id` bigint NOT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- --------------------------------------------------
-- Table structure for equipe_contrato
-- --------------------------------------------------
CREATE TABLE `equipe_contrato` (
  `id` int NOT NULL AUTO_INCREMENT,
  `ata_id` int DEFAULT NULL,
  `contrato_id` int DEFAULT NULL,
  `ativo_id` int NOT NULL,
  `criado_em` timestamp NULL DEFAULT CURRENT_TIMESTAMP,
  `atualizado_em` timestamp NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_ata_servidor_funcao` (`ata_id`),
  KEY `ativo_id` (`ativo_id`),
  KEY `fk_equipe_contrato_contrato` (`contrato_id`),
  CONSTRAINT `equipe_contrato_ibfk_1` FOREIGN KEY (`ata_id`) REFERENCES `ata_registro_preco` (`id`) ON DELETE CASCADE,
  CONSTRAINT `equipe_contrato_ibfk_4` FOREIGN KEY (`ativo_id`) REFERENCES `ativo` (`id`) ON DELETE RESTRICT,
  CONSTRAINT `fk_equipe_contrato_contrato` FOREIGN KEY (`contrato_id`) REFERENCES `contrato` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- --------------------------------------------------
-- Table structure for equipe_membro
-- --------------------------------------------------
CREATE TABLE `equipe_membro` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `equipe_id` int NOT NULL,
  `servidor_id` int NOT NULL,
  `funcao_id` int NOT NULL,
  PRIMARY KEY (`id`),
  KEY `idx_equipe_membro_equipe` (`equipe_id`),
  KEY `idx_equipe_membro_servidor` (`servidor_id`),
  KEY `idx_equipe_membro_funcao` (`funcao_id`),
  CONSTRAINT `fk_membro_equipe` FOREIGN KEY (`equipe_id`) REFERENCES `equipe_contrato` (`id`) ON DELETE CASCADE,
  CONSTRAINT `fk_membro_funcao` FOREIGN KEY (`funcao_id`) REFERENCES `funcao_equipe` (`id`),
  CONSTRAINT `fk_membro_servidor` FOREIGN KEY (`servidor_id`) REFERENCES `servidor` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- --------------------------------------------------
-- Table structure for faturamento_mensal
-- --------------------------------------------------
CREATE TABLE `faturamento_mensal` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `mes_referencia` int NOT NULL,
  `ano_referencia` int NOT NULL,
  `data_fechamento` date NOT NULL,
  `total_equipamentos` int NOT NULL DEFAULT '0',
  `total_copias_mono` int NOT NULL DEFAULT '0',
  `total_copias_color` int NOT NULL DEFAULT '0',
  `total_excedente_mono` int NOT NULL DEFAULT '0',
  `total_excedente_color` int NOT NULL DEFAULT '0',
  `valor_total_locacao` decimal(12,2) NOT NULL DEFAULT '0.00',
  `valor_total_excedentes` decimal(12,2) NOT NULL DEFAULT '0.00',
  `valor_total_fatura` decimal(12,2) NOT NULL DEFAULT '0.00',
  `status` varchar(30) COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'ABERTO',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- --------------------------------------------------
-- Table structure for feriado
-- --------------------------------------------------
CREATE TABLE `feriado` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `data` date NOT NULL,
  `descricao` varchar(100) COLLATE utf8mb4_unicode_ci NOT NULL,
  `tipo` varchar(30) COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'NACIONAL',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_feriado_data` (`data`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- --------------------------------------------------
-- Table structure for funcao_equipe
-- --------------------------------------------------
CREATE TABLE `funcao_equipe` (
  `id` int NOT NULL AUTO_INCREMENT,
  `nome` varchar(255) DEFAULT NULL,
  `ativo_id` int NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `nome` (`nome`),
  KEY `ativo_id` (`ativo_id`),
  CONSTRAINT `funcao_equipe_ibfk_1` FOREIGN KEY (`ativo_id`) REFERENCES `ativo` (`id`) ON DELETE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- --------------------------------------------------
-- Table structure for impressora
-- --------------------------------------------------
CREATE TABLE `impressora` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `ativo` bit(1) NOT NULL,
  `atualizado_em` datetime(6) NOT NULL,
  `criado_em` datetime(6) NOT NULL,
  `fabricante` varchar(100) NOT NULL,
  `ip` varchar(45) DEFAULT NULL,
  `item_pedido` int DEFAULT NULL,
  `modelo` varchar(100) NOT NULL,
  `numero_serie` varchar(100) DEFAULT NULL,
  `tipo_impressao` varchar(30) NOT NULL,
  `lote_id` bigint DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `FK3x5kexylnk8wmyof4d6993a3x` (`lote_id`),
  CONSTRAINT `FK3x5kexylnk8wmyof4d6993a3x` FOREIGN KEY (`lote_id`) REFERENCES `lote_impressao` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- --------------------------------------------------
-- Table structure for instalacao_impressora
-- --------------------------------------------------
CREATE TABLE `instalacao_impressora` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `contador_instalacao_color` int NOT NULL,
  `contador_instalacao_mono` int NOT NULL,
  `contador_retirada_color` int DEFAULT NULL,
  `contador_retirada_mono` int DEFAULT NULL,
  `data_instalacao` date NOT NULL,
  `data_retirada` date DEFAULT NULL,
  `endereco` varchar(255) DEFAULT NULL,
  `local_instalacao` varchar(255) NOT NULL,
  `motivo_retirada` varchar(255) DEFAULT NULL,
  `responsavel` varchar(150) DEFAULT NULL,
  `status` varchar(30) NOT NULL,
  `transformador` varchar(50) DEFAULT NULL,
  `empenho_id` bigint DEFAULT NULL,
  `impressora_id` bigint NOT NULL,
  `secretaria_id` bigint NOT NULL,
  `local_instalacao_id` bigint DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `FK4xpeqiddl65gho3koui014w9x` (`empenho_id`),
  KEY `FKp8w4lggsnug9kqs8bokhp1l9j` (`impressora_id`),
  KEY `idx_instalacao_local` (`local_instalacao_id`),
  CONSTRAINT `FK4xpeqiddl65gho3koui014w9x` FOREIGN KEY (`empenho_id`) REFERENCES `empenho_impressao` (`id`),
  CONSTRAINT `FKp8w4lggsnug9kqs8bokhp1l9j` FOREIGN KEY (`impressora_id`) REFERENCES `impressora` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- --------------------------------------------------
-- Table structure for leitura_contador
-- --------------------------------------------------
CREATE TABLE `leitura_contador` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `ano_referencia` int NOT NULL,
  `copias_color` int NOT NULL,
  `copias_mono` int NOT NULL,
  `data_leitura` date NOT NULL,
  `excedente_color` int NOT NULL,
  `excedente_mono` int NOT NULL,
  `franquia_color_aplicada` int NOT NULL,
  `franquia_mono_aplicada` int NOT NULL,
  `leitura_color_anterior` int NOT NULL,
  `leitura_color_atual` int NOT NULL,
  `leitura_mono_anterior` int NOT NULL,
  `leitura_mono_atual` int NOT NULL,
  `mes_referencia` int NOT NULL,
  `observacoes` varchar(255) DEFAULT NULL,
  `origem_leitura` varchar(30) NOT NULL,
  `proporcao` decimal(5,2) NOT NULL,
  `valor_excedente_color` decimal(10,2) NOT NULL,
  `valor_excedente_mono` decimal(10,2) NOT NULL,
  `valor_locacao` decimal(10,2) NOT NULL,
  `valor_total` decimal(10,2) NOT NULL,
  `impressora_id` bigint NOT NULL,
  `instalacao_id` bigint DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `FKa4veaw77k3ifycl594w4xhttn` (`impressora_id`),
  KEY `FKnsjrsep1o6e1fhlnolqew2oid` (`instalacao_id`),
  CONSTRAINT `FKa4veaw77k3ifycl594w4xhttn` FOREIGN KEY (`impressora_id`) REFERENCES `impressora` (`id`),
  CONSTRAINT `FKnsjrsep1o6e1fhlnolqew2oid` FOREIGN KEY (`instalacao_id`) REFERENCES `instalacao_impressora` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- --------------------------------------------------
-- Table structure for local_instalacao
-- --------------------------------------------------
CREATE TABLE `local_instalacao` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `ativo` bit(1) NOT NULL,
  `endereco` varchar(255) DEFAULT NULL,
  `nome` varchar(255) NOT NULL,
  `responsavel` varchar(150) DEFAULT NULL,
  `telefone` varchar(50) DEFAULT NULL,
  `secretaria_id` bigint NOT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- --------------------------------------------------
-- Table structure for lote_impressao
-- --------------------------------------------------
CREATE TABLE `lote_impressao` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `ativo` bit(1) NOT NULL,
  `descricao` varchar(255) NOT NULL,
  `franquia_color` int NOT NULL,
  `franquia_mono` int NOT NULL,
  `numero_lote` int NOT NULL,
  `tipo` varchar(50) NOT NULL,
  `valor_excedente_color` decimal(10,4) NOT NULL,
  `valor_excedente_mono` decimal(10,4) NOT NULL,
  `valor_locacao_mensal` decimal(10,2) NOT NULL,
  `contrato_id` bigint DEFAULT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- --------------------------------------------------
-- Table structure for notificacao_vencimento_enviada
-- --------------------------------------------------
CREATE TABLE `notificacao_vencimento_enviada` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `contrato_id` int DEFAULT NULL,
  `ata_id` int DEFAULT NULL,
  `dias_alerta` int NOT NULL,
  `data_fim_referencia` date NOT NULL,
  `enviado_em` datetime NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_notificacao_contrato_dias_datafim` (`contrato_id`,`dias_alerta`,`data_fim_referencia`),
  UNIQUE KEY `uk_notificacao_ata_dias_datafim` (`ata_id`,`dias_alerta`,`data_fim_referencia`),
  CONSTRAINT `fk_notificacao_ata` FOREIGN KEY (`ata_id`) REFERENCES `ata_registro_preco` (`id`),
  CONSTRAINT `fk_notificacao_contrato` FOREIGN KEY (`contrato_id`) REFERENCES `contrato` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- --------------------------------------------------
-- Table structure for periodo_aquisitivo
-- --------------------------------------------------
CREATE TABLE `periodo_aquisitivo` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `servidor_id` int NOT NULL,
  `ano_inicio` int NOT NULL,
  `ano_fim` int NOT NULL,
  `identificador` varchar(30) COLLATE utf8mb4_unicode_ci NOT NULL,
  `data_inicio` date NOT NULL,
  `data_fim` date NOT NULL,
  `limite_gozo` date DEFAULT NULL,
  `total_dias` int NOT NULL DEFAULT '30',
  `dias_usados` int NOT NULL DEFAULT '0',
  `dias_restantes` int NOT NULL DEFAULT '30',
  `cor_hex` varchar(20) COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT '#eab308',
  `criado_em` datetime DEFAULT CURRENT_TIMESTAMP,
  `atualizado_em` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `fk_periodo_servidor` (`servidor_id`),
  CONSTRAINT `fk_periodo_servidor` FOREIGN KEY (`servidor_id`) REFERENCES `servidor` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- --------------------------------------------------
-- Table structure for secretaria
-- --------------------------------------------------
CREATE TABLE `secretaria` (
  `id` int NOT NULL AUTO_INCREMENT,
  `nome` varchar(100) NOT NULL,
  `sigla` varchar(10) NOT NULL,
  `ativo_id` int NOT NULL,
  `criado_em` timestamp NULL DEFAULT CURRENT_TIMESTAMP,
  `atualizado_em` timestamp NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `sigla` (`sigla`),
  KEY `ativo_id` (`ativo_id`),
  CONSTRAINT `secretaria_ibfk_1` FOREIGN KEY (`ativo_id`) REFERENCES `ativo` (`id`) ON DELETE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- --------------------------------------------------
-- Table structure for servidor
-- --------------------------------------------------
CREATE TABLE `servidor` (
  `id` int NOT NULL AUTO_INCREMENT,
  `nome` varchar(200) NOT NULL,
  `email` varchar(100) NOT NULL,
  `cargo` varchar(50) NOT NULL DEFAULT '',
  `setor` varchar(100) DEFAULT NULL,
  `matricula` int NOT NULL,
  `telefone` varchar(20) NOT NULL,
  `secretaria_id` int DEFAULT NULL,
  `ativo_id` int NOT NULL,
  `criado_em` timestamp NULL DEFAULT CURRENT_TIMESTAMP,
  `atualizado_em` timestamp NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `secretaria_id` (`secretaria_id`),
  KEY `ativo_id` (`ativo_id`),
  CONSTRAINT `servidor_ibfk_1` FOREIGN KEY (`secretaria_id`) REFERENCES `secretaria` (`id`) ON DELETE RESTRICT,
  CONSTRAINT `servidor_ibfk_2` FOREIGN KEY (`ativo_id`) REFERENCES `ativo` (`id`) ON DELETE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- --------------------------------------------------
-- Table structure for tipo
-- --------------------------------------------------
CREATE TABLE `tipo` (
  `id` int NOT NULL AUTO_INCREMENT,
  `tipo_arp` varchar(50) NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `tipo_arp` (`tipo_arp`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- --------------------------------------------------
-- Table structure for usuario
-- --------------------------------------------------
CREATE TABLE `usuario` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `email` varchar(150) NOT NULL,
  `nome` varchar(150) NOT NULL,
  `papel` enum('ADMIN','GESTOR') NOT NULL,
  `senha` varchar(255) NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `UK5171l57faosmj8myawaucatdw` (`email`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;


SET FOREIGN_KEY_CHECKS = 1;
