package com.contract_management.api.modules.ferias.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.*;
import java.util.stream.Collectors;

import com.contract_management.api.modules.ferias.dto.response.EscalaAnualDTO;
import com.contract_management.api.modules.ferias.model.AgendamentoFerias;
import com.contract_management.api.modules.ferias.model.Feriado;
import com.contract_management.api.modules.ferias.repository.AgendamentoFeriasRepository;
import com.contract_management.api.modules.ferias.repository.FeriadoRepository;
import com.contract_management.api.modules.secretaria.model.Secretaria;
import com.contract_management.api.modules.secretaria.repository.SecretariaRepository;
import com.contract_management.api.modules.servidor.model.Servidor;

@Service
@RequiredArgsConstructor
@Slf4j
public class EscalaFeriasService {

    private final AgendamentoFeriasRepository agendamentoRepository;
    private final FeriadoRepository feriadoRepository;
    private final SecretariaRepository secretariaRepository;

    private static final String[] NOMES_MESES = {
            "Janeiro", "Fevereiro", "Março", "Abril", "Maio", "Junho",
            "Julho", "Agosto", "Setembro", "Outubro", "Novembro", "Dezembro"
    };

    private static final String[] DIAS_SEMANA_SIGLAS = {
            "Seg", "Ter", "Qua", "Qui", "Sex", "Sáb", "Dom"
    };

    @Transactional(readOnly = true)
    public EscalaAnualDTO gerarEscalaAnual(Integer ano, Long secretariaId, String setor) {
        log.info("Gerando escala anual para ano: {}, secretaria: {}, setor: {}", ano, secretariaId, setor);

        if (ano == null) {
            ano = LocalDate.now().getYear();
        }

        LocalDate inicioAno = LocalDate.of(ano, 1, 1);
        LocalDate fimAno = LocalDate.of(ano, 12, 31);

        // Feriados do ano
        Map<LocalDate, Feriado> feriadosMap = feriadoRepository.findByDataBetweenOrderByDataAsc(inicioAno, fimAno)
                .stream().collect(Collectors.toMap(Feriado::getData, f -> f, (f1, f2) -> f1));

        // Todos os agendamentos do ano
        List<AgendamentoFerias> todosAgendamentos = agendamentoRepository.findParaEscalaAnual(inicioAno, fimAno, secretariaId, setor);

        // Obter dados da secretaria se informada
        String secretariaNome = null;
        String secretariaSigla = null;
        if (secretariaId != null) {
            Optional<Secretaria> secOpt = secretariaRepository.findById(secretariaId);
            if (secOpt.isPresent()) {
                secretariaNome = secOpt.get().getNome();
                secretariaSigla = secOpt.get().getSigla();
            }
        }

        List<EscalaAnualDTO.MesEscalaDTO> mesesList = new ArrayList<>();
        Map<String, String> periodosAquisitivosCores = new LinkedHashMap<>();

        // Construir cada mês de 1 a 12
        for (int m = 1; m <= 12; m++) {
            YearMonth ym = YearMonth.of(ano, m);
            int totalDias = ym.lengthOfMonth();
            LocalDate inicioMes = LocalDate.of(ano, m, 1);
            LocalDate fimMes = LocalDate.of(ano, m, totalDias);

            // Informações dos dias do mês
            List<EscalaAnualDTO.DiaInfoDTO> diasInfo = new ArrayList<>();
            for (int d = 1; d <= totalDias; d++) {
                LocalDate data = LocalDate.of(ano, m, d);
                DayOfWeek dow = data.getDayOfWeek();
                boolean ehFimDeSemana = (dow == DayOfWeek.SATURDAY || dow == DayOfWeek.SUNDAY);
                Feriado feriado = feriadosMap.get(data);
                boolean ehFeriado = (feriado != null);
                String nomeFeriado = ehFeriado ? feriado.getDescricao() : null;

                diasInfo.add(EscalaAnualDTO.DiaInfoDTO.builder()
                        .dia(d)
                        .diaSemana(dow.getValue())
                        .diaSemanaSigla(DIAS_SEMANA_SIGLAS[dow.getValue() - 1])
                        .ehFimDeSemana(ehFimDeSemana)
                        .ehFeriado(ehFeriado)
                        .nomeFeriado(nomeFeriado)
                        .build());
            }

            // Filtrar agendamentos que interceptam este mês
            List<AgendamentoFerias> agendamentosNoMes = todosAgendamentos.stream()
                    .filter(a -> !a.getDataInicio().isAfter(fimMes) && !a.getDataFim().isBefore(inicioMes))
                    .collect(Collectors.toList());

            // Agrupar por Servidor (mantendo ordem por nome do servidor)
            Map<Servidor, List<AgendamentoFerias>> porServidor = agendamentosNoMes.stream()
                    .collect(Collectors.groupingBy(
                            AgendamentoFerias::getServidor,
                            LinkedHashMap::new,
                            Collectors.toList()
                    ));

            List<EscalaAnualDTO.LinhaServidorMesDTO> linhasServidores = new ArrayList<>();

            for (Map.Entry<Servidor, List<AgendamentoFerias>> entry : porServidor.entrySet()) {
                Servidor servidor = entry.getKey();
                List<AgendamentoFerias> lista = entry.getValue();

                List<EscalaAnualDTO.CelulaDiaDTO> celulas = new ArrayList<>();

                for (int d = 1; d <= totalDias; d++) {
                    LocalDate data = LocalDate.of(ano, m, d);

                    // Achar agendamento que cobre esta data
                    Optional<AgendamentoFerias> agOpt = lista.stream()
                            .filter(a -> !data.isBefore(a.getDataInicio()) && !data.isAfter(a.getDataFim()))
                            .findFirst();

                    if (agOpt.isPresent()) {
                        AgendamentoFerias a = agOpt.get();
                        String corHex = a.getPeriodoAquisitivo() != null
                                ? a.getPeriodoAquisitivo().getCorHex()
                                : a.getTipoAfastamento().getCorPadrao();

                        String periodoIdentificador = a.getPeriodoAquisitivo() != null
                                ? a.getPeriodoAquisitivo().getIdentificador()
                                : null;

                        if (a.getPeriodoAquisitivo() != null) {
                            periodosAquisitivosCores.putIfAbsent(
                                    "Período aquisitivo " + periodoIdentificador,
                                    corHex
                            );
                        }

                        celulas.add(EscalaAnualDTO.CelulaDiaDTO.builder()
                                .dia(d)
                                .ocupado(true)
                                .agendamentoId(a.getId())
                                .tipoAfastamento(a.getTipoAfastamento().name())
                                .tipoDescricao(a.getTipoAfastamento().getDescricao())
                                .periodoAquisitivoId(a.getPeriodoAquisitivo() != null ? a.getPeriodoAquisitivo().getId() : null)
                                .periodoIdentificador(periodoIdentificador)
                                .corHex(corHex)
                                .fracao(a.getFracao())
                                .status(a.getStatus().name())
                                .alertaConflito(Boolean.TRUE.equals(a.getAlertaConflito()))
                                .descricaoConflito(a.getDescricaoConflito())
                                .observacao(a.getObservacao())
                                .build());
                    } else {
                        celulas.add(EscalaAnualDTO.CelulaDiaDTO.builder()
                                .dia(d)
                                .ocupado(false)
                                .build());
                    }
                }

                linhasServidores.add(EscalaAnualDTO.LinhaServidorMesDTO.builder()
                        .servidorId(servidor.getId())
                        .servidorNome(servidor.getNome())
                        .servidorMatricula(servidor.getMatricula())
                        .servidorCargo(servidor.getCargo())
                        .servidorSetor(servidor.getSetor())
                        .celulas(celulas)
                        .build());
            }

            mesesList.add(EscalaAnualDTO.MesEscalaDTO.builder()
                    .mesNumero(m)
                    .mesNome(NOMES_MESES[m - 1] + "/" + ano)
                    .totalDias(totalDias)
                    .dias(diasInfo)
                    .linhas(linhasServidores)
                    .build());
        }

        // Construir legendas
        List<EscalaAnualDTO.LegendaItemDTO> legendas = new ArrayList<>();
        legendas.add(EscalaAnualDTO.LegendaItemDTO.builder()
                .label("Fim de semana e Feriados")
                .corHex("#94a3b8")
                .tipo("SISTEMA")
                .build());

        // Adicionar períodos aquisitivos encontrados ordenados
        periodosAquisitivosCores.forEach((label, cor) ->
                legendas.add(EscalaAnualDTO.LegendaItemDTO.builder()
                        .label(label)
                        .corHex(cor)
                        .tipo("PERIODO")
                        .build())
        );

        // Afastamentos padrão da foto
        legendas.add(EscalaAnualDTO.LegendaItemDTO.builder()
                .label("Licença Saúde")
                .corHex("#ef4444")
                .tipo("AFASTAMENTO")
                .build());
        legendas.add(EscalaAnualDTO.LegendaItemDTO.builder()
                .label("Licença Prêmio")
                .corHex("#b45309")
                .tipo("AFASTAMENTO")
                .build());
        legendas.add(EscalaAnualDTO.LegendaItemDTO.builder()
                .label("Folgas")
                .corHex("#4338ca")
                .tipo("AFASTAMENTO")
                .build());

        return EscalaAnualDTO.builder()
                .ano(ano)
                .secretariaId(secretariaId)
                .secretariaNome(secretariaNome)
                .secretariaSigla(secretariaSigla)
                .setor(setor)
                .meses(mesesList)
                .legendas(legendas)
                .build();
    }
}