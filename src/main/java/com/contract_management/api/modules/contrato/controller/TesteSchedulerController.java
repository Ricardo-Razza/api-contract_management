package com.contract_management.api.modules.contrato.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import com.contract_management.api.modules.contrato.scheduler.NotificacaoVencimentoScheduler;

import java.util.Map;

@RestController
@RequestMapping("/teste")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Testes Administrativos", description = "Endpoints para testes manuais e disparo de notificações")
public class TesteSchedulerController {

    private final NotificacaoVencimentoScheduler scheduler;

    @RequestMapping(value = "/verificar-contratos", method = {RequestMethod.GET, RequestMethod.POST})
    @Operation(summary = "Dispara manualmente a verificação e envio de alertas de contratos vencendo")
    public ResponseEntity<Map<String, String>> verificarContratosAgora() {
        log.info("Disparo manual de verificação de contratos vencendo solicitado via /teste/verificar-contratos");
        scheduler.verificarContratosVencendo();
        return ResponseEntity.ok(Map.of(
                "status", "SUCESSO",
                "mensagem", "Verificação de contratos executada com sucesso. Confira os logs para detalhes dos e-mails enviados."
        ));
    }

    @RequestMapping(value = "/verificar-atas", method = {RequestMethod.GET, RequestMethod.POST})
    @Operation(summary = "Dispara manualmente a verificação e envio de alertas de atas vencendo")
    public ResponseEntity<Map<String, String>> verificarAtasAgora() {
        log.info("Disparo manual de verificação de atas vencendo solicitado via /teste/verificar-atas");
        scheduler.verificarAtasVencendo();
        return ResponseEntity.ok(Map.of(
                "status", "SUCESSO",
                "mensagem", "Verificação de atas executada com sucesso. Confira os logs para detalhes dos e-mails enviados."
        ));
    }
}