-- Migration V2: Constraint única para notificações enviadas
ALTER TABLE `notificacao_vencimento_enviada`
    ADD CONSTRAINT `uk_notificacao_contrato_dias` UNIQUE (`contrato_id`, `dias_alerta`);

ALTER TABLE `notificacao_vencimento_enviada`
    ADD CONSTRAINT `uk_notificacao_ata_dias` UNIQUE (`ata_id`, `dias_alerta`);
