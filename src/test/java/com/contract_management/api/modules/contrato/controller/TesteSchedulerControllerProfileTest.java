package com.contract_management.api.modules.contrato.controller;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import com.contract_management.api.modules.contrato.scheduler.NotificacaoVencimentoScheduler;

import static org.assertj.core.api.Assertions.assertThat;

class TesteSchedulerControllerProfileTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withBean(NotificacaoVencimentoScheduler.class, () -> Mockito.mock(NotificacaoVencimentoScheduler.class))
            .withUserConfiguration(TesteSchedulerController.class);

    @Test
    @DisplayName("Não deve registrar TesteSchedulerController quando profile for prod")
    void deveIgnorarControllerEmProfileProd() {
        contextRunner
                .withPropertyValues("spring.profiles.active=prod")
                .run(context -> assertThat(context).doesNotHaveBean(TesteSchedulerController.class));
    }

    @Test
    @DisplayName("Deve registrar TesteSchedulerController quando profile for dev")
    void deveRegistrarControllerEmProfileDev() {
        contextRunner
                .withPropertyValues("spring.profiles.active=dev")
                .run(context -> assertThat(context).hasSingleBean(TesteSchedulerController.class));
    }
}
