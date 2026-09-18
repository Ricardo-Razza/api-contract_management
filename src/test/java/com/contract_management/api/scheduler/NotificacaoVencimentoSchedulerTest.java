package com.contract_management.api.scheduler;

import com.contract_management.api.model.*;
import com.contract_management.api.repository.*;
import com.contract_management.api.service.EmailAlertaService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.Collections;
import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class NotificacaoVencimentoSchedulerTest {

    @Mock
    private ContratoRepository contratoRepository;

    @Mock
    private EquipeContratoRepository equipeContratoRepository;

    @Mock
    private NotificacaoVencimentoEnviadaRepository notificacaoRepository;

    @Mock
    private AtaRepository ataRepository;

    @Mock
    private EmailAlertaService emailAlertaService;

    @InjectMocks
    private NotificacaoVencimentoScheduler scheduler;

    private Contrato contrato;
    private EquipeContrato equipe;
    private Servidor servidor;

    @BeforeEach
    void setUp() {
        contrato = Contrato.builder()
                .id(1L)
                .numero(100)
                .ano(2026)
                .dataFim(LocalDate.now().plusDays(90))
                .build();

        servidor = Servidor.builder()
                .id(1L)
                .nome("João da Silva")
                .email("joao@exemplo.com")
                .build();

        EquipeMembro membro = EquipeMembro.builder()
                .id(1L)
                .servidor(servidor)
                .build();

        equipe = EquipeContrato.builder()
                .id(1L)
                .contrato(contrato)
                .membros(List.of(membro))
                .build();
    }

    @Test
    void deveRegistrarNotificacaoQuandoEmailForEnviadoComSucesso() {
        when(contratoRepository.findByDataFim(any(LocalDate.class)))
                .thenReturn(List.of(contrato))
                .thenReturn(Collections.emptyList())
                .thenReturn(Collections.emptyList());

        when(notificacaoRepository.findContratoIdsJaNotificados(anyList(), anyInt()))
                .thenReturn(Collections.emptyList());

        when(equipeContratoRepository.findByContratoIdInComMembros(anyList()))
                .thenReturn(List.of(equipe));

        // Email enviado com sucesso (sem lançar exceção)
        doNothing().when(emailAlertaService).enviarAlertaVencimento(anyString(), anyString(), anyInt(), anyInt(), anyLong());

        scheduler.verificarContratosVencendo();

        // Verifica que o envio foi chamado e o registro foi salvo no banco
        verify(emailAlertaService, times(1)).enviarAlertaVencimento(eq("joao@exemplo.com"), anyString(), eq(100), eq(2026), eq(90L));
        verify(notificacaoRepository, times(1)).save(any(NotificacaoVencimentoEnviada.class));
    }

    @Test
    void naoDeveRegistrarNotificacaoQuandoEnvioDeEmailFalhar() {
        when(contratoRepository.findByDataFim(any(LocalDate.class)))
                .thenReturn(List.of(contrato))
                .thenReturn(Collections.emptyList())
                .thenReturn(Collections.emptyList());

        when(notificacaoRepository.findContratoIdsJaNotificados(anyList(), anyInt()))
                .thenReturn(Collections.emptyList());

        when(equipeContratoRepository.findByContratoIdInComMembros(anyList()))
                .thenReturn(List.of(equipe));

        // Simulando falha no serviço de e-mail (exceção lançada)
        doThrow(new RuntimeException("Falha SMTP")).when(emailAlertaService)
                .enviarAlertaVencimento(anyString(), anyString(), anyInt(), anyInt(), anyLong());

        scheduler.verificarContratosVencendo();

        // O envio tentou ser realizado...
        verify(emailAlertaService, times(1)).enviarAlertaVencimento(anyString(), anyString(), anyInt(), anyInt(), anyLong());
        // Mas a notificação NÃO DEVE ser registrada como enviada no banco!
        verify(notificacaoRepository, never()).save(any(NotificacaoVencimentoEnviada.class));
    }
}
