package com.contract_management.api.controller;

import com.contract_management.api.scheduler.NotificacaoVencimentoScheduler;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/teste")
@RequiredArgsConstructor
public class TesteSchedulerController {

    private final NotificacaoVencimentoScheduler scheduler;

    @PostMapping("/verificar-contratos")
    public String verificarContratosAgora() {
        scheduler.verificarContratosVencendo();
        return "Verificação executada. Confira os logs.";
    }

    @PostMapping("/verificar-atas")
    public String verificarAtasAgora(){
        scheduler.verificarAtasVencendo();
        return "Verificação executada. Confira os logs.";
    }


}