package com.contract_management.api.scheduler;

import com.contract_management.api.model.AtaRegistroPreco;
import com.contract_management.api.model.Contrato;
import com.contract_management.api.model.EquipeContrato;
import com.contract_management.api.model.EquipeMembro;
import com.contract_management.api.model.NotificacaoVencimentoEnviada;
import com.contract_management.api.model.Servidor;
import com.contract_management.api.repository.*;
import com.contract_management.api.service.EmailAlertaService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
@Slf4j
public class NotificacaoVencimentoScheduler {

    private final ContratoRepository contratoRepository;
    private final EquipeContratoRepository equipeContratoRepository;
    private final NotificacaoVencimentoEnviadaRepository notificacaoRepository;
    private final AtaRepository ataRepository;
    private final EmailAlertaService emailAlertaService;

    private static final int[] DIAS_ALERTA = {90, 60, 30};

    // ===================== CONTRATOS =====================

    @Scheduled(cron = "0 0 8 * * *")
    @Transactional
    public void verificarContratosVencendo() {
        for (int dias : DIAS_ALERTA) {
            LocalDate dataAlvo = LocalDate.now().plusDays(dias);
            List<Contrato> contratos = contratoRepository.findByDataFim(dataAlvo);

            if (contratos.isEmpty()) {
                continue;
            }

            List<Long> todosIds = contratos.stream()
                    .map(Contrato::getId)
                    .collect(Collectors.toList());

            List<Long> jaNotificados = notificacaoRepository
                    .findContratoIdsJaNotificados(todosIds, dias);

            List<Contrato> contratosPendentes = contratos.stream()
                    .filter(c -> !jaNotificados.contains(c.getId()))
                    .collect(Collectors.toList());

            if (contratosPendentes.isEmpty()) {
                log.info("Todos os contratos vencendo em {} dias já foram notificados.", dias);
                continue;
            }

            List<Long> idsPendentes = contratosPendentes.stream()
                    .map(Contrato::getId)
                    .collect(Collectors.toList());

            List<EquipeContrato> equipes = equipeContratoRepository
                    .findByContratoIdInComMembros(idsPendentes);

            Map<Long, List<EquipeContrato>> equipesPorContrato = equipes.stream()
                    .collect(Collectors.groupingBy(eq -> eq.getContrato().getId()));

            for (Contrato contrato : contratosPendentes) {
                List<EquipeContrato> equipesDoContrato =
                        equipesPorContrato.getOrDefault(contrato.getId(), List.of());

                notificarEquipeContrato(contrato, equipesDoContrato, dias);
                registrarNotificacaoContrato(contrato, dias);
            }
        }
    }

    private void notificarEquipeContrato(Contrato contrato, List<EquipeContrato> equipes, int diasRestantes) {
        if (equipes.isEmpty()) {
            log.warn("Contrato {}/{} vence em {} dias mas não tem equipe vinculada",
                    contrato.getNumero(), contrato.getAno(), diasRestantes);
            return;
        }

        for (EquipeContrato equipe : equipes) {
            for (EquipeMembro membro : equipe.getMembros()) {
                Servidor servidor = membro.getServidor();
                try {
                    emailAlertaService.enviarAlertaVencimento(
                            servidor.getEmail(),
                            servidor.getNome(),
                            contrato.getNumero(),
                            contrato.getAno(),
                            diasRestantes
                    );
                    log.info("Alerta de {} dias enviado para {} (contrato {}/{})",
                            diasRestantes, servidor.getEmail(),
                            contrato.getNumero(), contrato.getAno());
                } catch (Exception e) {
                    log.error("Falha ao enviar e-mail para {} (servidor id={}, contrato {}/{}): {}",
                            servidor.getEmail(), servidor.getId(),
                            contrato.getNumero(), contrato.getAno(), e.getMessage());
                }
            }
        }
    }

    private void registrarNotificacaoContrato(Contrato contrato, int diasAlerta) {
        NotificacaoVencimentoEnviada registro = NotificacaoVencimentoEnviada.builder()
                .contrato(contrato)
                .diasAlerta(diasAlerta)
                .dataFimReferencia(contrato.getDataFim())
                .enviadoEm(LocalDateTime.now())
                .build();

        notificacaoRepository.save(registro);
    }

    // ===================== ATAS DE REGISTRO DE PREÇO =====================

    @Scheduled(cron = "0 30 8 * * *")
    @Transactional
    public void verificarAtasVencendo() {
        for (int dias : DIAS_ALERTA) {
            LocalDate dataAlvo = LocalDate.now().plusDays(dias);
            List<AtaRegistroPreco> atas = ataRepository.findByDataFim(dataAlvo);

            if (atas.isEmpty()) {
                continue;
            }

            List<Long> todosIds = atas.stream()
                    .map(AtaRegistroPreco::getId)
                    .collect(Collectors.toList());

            List<Long> jaNotificados = notificacaoRepository
                    .findAtaIdsJaNotificados(todosIds, dias);

            List<AtaRegistroPreco> atasPendentes = atas.stream()
                    .filter(a -> !jaNotificados.contains(a.getId()))
                    .collect(Collectors.toList());

            if (atasPendentes.isEmpty()) {
                log.info("Todas as atas vencendo em {} dias já foram notificadas.", dias);
                continue;
            }

            List<Long> idsPendentes = atasPendentes.stream()
                    .map(AtaRegistroPreco::getId)
                    .collect(Collectors.toList());

            List<EquipeContrato> equipes = equipeContratoRepository
                    .findByAtaIdInComMembros(idsPendentes);

            Map<Long, List<EquipeContrato>> equipesPorAta = equipes.stream()
                    .collect(Collectors.groupingBy(eq -> eq.getAta().getId()));

            for (AtaRegistroPreco ata : atasPendentes) {
                List<EquipeContrato> equipesDaAta =
                        equipesPorAta.getOrDefault(ata.getId(), List.of());

                notificarEquipeAta(ata, equipesDaAta, dias);
                registrarNotificacaoAta(ata, dias);
            }
        }
    }

    private void notificarEquipeAta(AtaRegistroPreco ata, List<EquipeContrato> equipes, int diasRestantes) {
        if (equipes.isEmpty()) {
            log.warn("Ata {}/{} vence em {} dias mas não tem equipe vinculada",
                    ata.getNumero(), ata.getAno(), diasRestantes);
            return;
        }

        for (EquipeContrato equipe : equipes) {
            for (EquipeMembro membro : equipe.getMembros()) {
                Servidor servidor = membro.getServidor();
                try {
                    emailAlertaService.enviarAlertaVencimentoAta(
                            servidor.getEmail(),
                            servidor.getNome(),
                            ata.getNumero(),
                            ata.getAno(),
                            diasRestantes
                    );
                    log.info("Alerta de {} dias enviado para {} (ata {}/{})",
                            diasRestantes, servidor.getEmail(),
                            ata.getNumero(), ata.getAno());
                } catch (Exception e) {
                    log.error("Falha ao enviar e-mail para {} (servidor id={}, ata {}/{}): {}",
                            servidor.getEmail(), servidor.getId(),
                            ata.getNumero(), ata.getAno(), e.getMessage());
                }
            }
        }
    }

    private void registrarNotificacaoAta(AtaRegistroPreco ata, int diasAlerta) {
        NotificacaoVencimentoEnviada registro = NotificacaoVencimentoEnviada.builder()
                .ata(ata)
                .diasAlerta(diasAlerta)
                .dataFimReferencia(ata.getDataFim())
                .enviadoEm(LocalDateTime.now())
                .build();

        notificacaoRepository.save(registro);
    }
}