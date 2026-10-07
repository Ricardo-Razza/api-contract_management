package com.contract_management.api.modules.ferias.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.stream.Collectors;

import com.contract_management.api.common.exception.BusinessException;
import com.contract_management.api.common.exception.EntityNotFoundException;
import com.contract_management.api.modules.ferias.dto.request.AgendamentoFeriasRequestDTO;
import com.contract_management.api.modules.ferias.dto.request.VerificacaoConflitoRequestDTO;
import com.contract_management.api.modules.ferias.dto.response.AgendamentoFeriasResponseDTO;
import com.contract_management.api.modules.ferias.dto.response.ConflitoItemDTO;
import com.contract_management.api.modules.ferias.dto.response.VerificacaoConflitoResponseDTO;
import com.contract_management.api.modules.ferias.model.AgendamentoFerias;
import com.contract_management.api.modules.ferias.model.PeriodoAquisitivo;
import com.contract_management.api.modules.ferias.model.StatusFerias;
import com.contract_management.api.modules.ferias.model.TipoAfastamento;
import com.contract_management.api.modules.ferias.repository.AgendamentoFeriasRepository;
import com.contract_management.api.modules.ferias.repository.PeriodoAquisitivoRepository;
import com.contract_management.api.modules.servidor.model.Servidor;
import com.contract_management.api.modules.servidor.api.ServidorConsulta;

@Service
@RequiredArgsConstructor
@Slf4j
public class AgendamentoFeriasService {

    private final AgendamentoFeriasRepository agendamentoRepository;
    private final ServidorConsulta servidorRepository;
    private final PeriodoAquisitivoRepository periodoRepository;
    private final PeriodoAquisitivoService periodoService;

    @Transactional(readOnly = true)
    public List<AgendamentoFeriasResponseDTO> listarPorServidor(Long servidorId) {
        log.info("Listando agendamentos do servidor ID: {}", servidorId);
        return agendamentoRepository.findByServidorIdOrderByDataInicioAsc(servidorId).stream()
                .map(this::toResponseDTO)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public AgendamentoFeriasResponseDTO buscarPorId(Long id) {
        log.info("Buscando agendamento ID: {}", id);
        AgendamentoFerias agendamento = agendamentoRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Agendamento de Férias", id));
        return toResponseDTO(agendamento);
    }

    @Transactional(readOnly = true)
    public VerificacaoConflitoResponseDTO verificarConflitos(VerificacaoConflitoRequestDTO dto) {
        Servidor servidor = servidorRepository.findById(dto.getServidorId())
                .orElseThrow(() -> new EntityNotFoundException("Servidor", dto.getServidorId()));

        Long secId = servidor.getSecretaria() != null ? servidor.getSecretaria().getId() : null;
        String setor = servidor.getSetor();

        List<AgendamentoFerias> conflitos = agendamentoRepository.findConflitos(
                servidor.getId(),
                dto.getDataInicio(),
                dto.getDataFim(),
                secId,
                setor,
                dto.getAgendamentoId()
        );

        if (conflitos.isEmpty()) {
            return VerificacaoConflitoResponseDTO.builder()
                    .temConflito(false)
                    .mensagem("Nenhum conflito de escala detectado no setor/secretaria.")
                    .build();
        }

        List<ConflitoItemDTO> itens = conflitos.stream().map(c -> ConflitoItemDTO.builder()
                .agendamentoId(c.getId())
                .servidorId(c.getServidor().getId())
                .servidorNome(c.getServidor().getNome())
                .servidorMatricula(c.getServidor().getMatricula())
                .setor(c.getServidor().getSetor())
                .tipoAfastamento(c.getTipoAfastamento().getDescricao())
                .dataInicio(c.getDataInicio())
                .dataFim(c.getDataFim())
                .dias(c.getDias())
                .build()
        ).collect(Collectors.toList());

        String nomes = conflitos.stream()
                .map(c -> c.getServidor().getNome() + " (" + c.getDataInicio() + " a " + c.getDataFim() + ")")
                .collect(Collectors.joining(", "));

        return VerificacaoConflitoResponseDTO.builder()
                .temConflito(true)
                .bloqueante(conflitos.stream().anyMatch(c -> c.getServidor().getId().equals(servidor.getId())))
                .mensagem("Atenção: Há sobreposição de afastamento com: " + nomes)
                .conflitos(itens)
                .build();
    }

    @Transactional(readOnly = true)
    public List<AgendamentoFeriasResponseDTO> listar(Integer ano) {
        int referencia = ano == null ? java.time.LocalDate.now().getYear() : ano;
        if (referencia < 1900 || referencia > 2200) throw new BusinessException("Ano inválido.");
        return agendamentoRepository.listarIntervalo(java.time.LocalDate.of(referencia, 1, 1),
                java.time.LocalDate.of(referencia, 12, 31)).stream().map(this::toResponseDTO).toList();
    }

    @Transactional(isolation = org.springframework.transaction.annotation.Isolation.READ_COMMITTED)
    public AgendamentoFeriasResponseDTO criar(AgendamentoFeriasRequestDTO dto) {
        Servidor servidor = bloquearServidor(dto.getServidorId());
        return gravar(new AgendamentoFerias(), servidor, dto);
    }

    @Transactional(isolation = org.springframework.transaction.annotation.Isolation.READ_COMMITTED)
    public AgendamentoFeriasResponseDTO atualizar(Long id, AgendamentoFeriasRequestDTO dto) {
        AgendamentoFerias ag = buscarAgendamento(id);
        if (!ag.getServidor().getId().equals(dto.getServidorId())) {
            throw new BusinessException("O servidor de um agendamento não pode ser alterado. Cancele e crie outro agendamento.");
        }
        Servidor servidor = bloquearServidor(dto.getServidorId());
        // Read again under the row lock so concurrent edits cannot refund twice.
        entityManager.refresh(ag);
        if (ag.getStatus() == StatusFerias.CANCELADO) throw new BusinessException("Agendamento cancelado não pode ser editado.");
        return gravar(ag, servidor, dto);
    }

    private final jakarta.persistence.EntityManager entityManager;

    private Servidor bloquearServidor(Long id) {
        return servidorRepository.bloquearPorId(id).orElseThrow(() -> new EntityNotFoundException("Servidor", id));
    }

    private AgendamentoFerias buscarAgendamento(Long id) {
        return agendamentoRepository.findById(id).orElseThrow(() -> new EntityNotFoundException("Agendamento de Férias", id));
    }

    private AgendamentoFeriasResponseDTO gravar(AgendamentoFerias ag, Servidor servidor, AgendamentoFeriasRequestDTO dto) {
        if (dto.getDataInicio() == null || dto.getDataFim() == null || dto.getDataFim().isBefore(dto.getDataInicio()))
            throw new BusinessException("Informe datas válidas: o término deve ser igual ou posterior ao início.");
        int dias = Math.toIntExact(ChronoUnit.DAYS.between(dto.getDataInicio(), dto.getDataFim()) + 1);
        if (dto.getTipoAfastamento() == TipoAfastamento.FERIAS && dias > 30)
            throw new BusinessException("Cada agendamento de férias pode ter no máximo 30 dias.");
        if (dto.getFracao() != null && (dto.getFracao() < 1 || dto.getFracao() > 3))
            throw new BusinessException("Fração deve estar entre 1 e 3.");
        StatusFerias status = dto.getStatus() == null ? StatusFerias.PLANEJADO : dto.getStatus();
        if (status == StatusFerias.CANCELADO) throw new BusinessException("Utilize a ação de cancelar agendamento.");
        var conflitos = agendamentoRepository.findConflitos(servidor.getId(), dto.getDataInicio(), dto.getDataFim(),
                servidor.getSecretaria() == null ? null : servidor.getSecretaria().getId(), servidor.getSetor(), ag.getId());
        if (conflitos.stream().anyMatch(c -> c.getServidor().getId().equals(servidor.getId())))
            throw new BusinessException("Este servidor já possui um afastamento nessas datas.");
        if (!conflitos.isEmpty() && !Boolean.TRUE.equals(dto.getConfirmarComConflito()))
            throw new BusinessException("CONFLITO_ESCALA:Há outros servidores do mesmo setor afastados nessas datas.");
        PeriodoAquisitivo periodo = null;
        if (dto.getTipoAfastamento() == TipoAfastamento.FERIAS) {
            if (dto.getPeriodoAquisitivoId() == null) throw new BusinessException("Selecione o período aquisitivo.");
            periodo = periodoRepository.bloquearPorId(dto.getPeriodoAquisitivoId())
                    .orElseThrow(() -> new EntityNotFoundException("Período Aquisitivo", dto.getPeriodoAquisitivoId()));
            entityManager.refresh(periodo);
            if (!periodo.getServidor().getId().equals(servidor.getId()))
                throw new BusinessException("O período aquisitivo não pertence a este servidor.");
        }
        if (ag.getPeriodoAquisitivo() != null && ag.getStatus() != StatusFerias.CANCELADO) {
            var antigo = periodoRepository.bloquearPorId(ag.getPeriodoAquisitivo().getId()).orElseThrow();
            if (periodo == null || !antigo.getId().equals(periodo.getId())) entityManager.refresh(antigo);
            periodoService.estornarDias(antigo, ag.getDias());
        }
        if (periodo != null) periodoService.debitarDias(periodo, dias);
        ag.setServidor(servidor);
        ag.setPeriodoAquisitivo(periodo);
        ag.setTipoAfastamento(dto.getTipoAfastamento());
        ag.setDataInicio(dto.getDataInicio());
        ag.setDataFim(dto.getDataFim());
        ag.setDias(dias);
        ag.setFracao(periodo == null ? null : dto.getFracao());
        ag.setStatus(status);
        ag.setObservacao(dto.getObservacao());
        ag.setAlertaConflito(!conflitos.isEmpty());
        ag.setDescricaoConflito(null);
        return toResponseDTO(agendamentoRepository.saveAndFlush(ag));
    }

    @Transactional(isolation = org.springframework.transaction.annotation.Isolation.READ_COMMITTED)
    public AgendamentoFeriasResponseDTO cancelar(Long id) {
        var ag = buscarAgendamento(id);
        bloquearServidor(ag.getServidor().getId());
        entityManager.refresh(ag);
        if (ag.getStatus() != StatusFerias.CANCELADO) {
            if (ag.getPeriodoAquisitivo() != null) {
                var periodo = periodoRepository.bloquearPorId(ag.getPeriodoAquisitivo().getId()).orElseThrow();
                entityManager.refresh(periodo);
                periodoService.estornarDias(periodo, ag.getDias());
            }
            ag.setStatus(StatusFerias.CANCELADO);
            ag.setAlertaConflito(false);
            ag.setDescricaoConflito(null);
            agendamentoRepository.saveAndFlush(ag);
        }
        return toResponseDTO(ag);
    }

    @Transactional(isolation = org.springframework.transaction.annotation.Isolation.READ_COMMITTED)
    public void deletar(Long id) {
        cancelar(id);
        agendamentoRepository.deleteById(id);
    }

    public AgendamentoFeriasResponseDTO toResponseDTO(AgendamentoFerias a) {
        AgendamentoFeriasResponseDTO dto = new AgendamentoFeriasResponseDTO();
        dto.setId(a.getId());
        dto.setServidorId(a.getServidor().getId());
        dto.setServidorNome(a.getServidor().getNome());
        dto.setServidorMatricula(a.getServidor().getMatricula());
        dto.setServidorCargo(a.getServidor().getCargo());
        dto.setServidorSetor(a.getServidor().getSetor());

        if (a.getServidor().getSecretaria() != null) {
            dto.setSecretariaId(a.getServidor().getSecretaria().getId());
            dto.setSecretariaNome(a.getServidor().getSecretaria().getNome());
            dto.setSecretariaSigla(a.getServidor().getSecretaria().getSigla());
        }

        if (a.getPeriodoAquisitivo() != null) {
            dto.setPeriodoAquisitivoId(a.getPeriodoAquisitivo().getId());
            dto.setPeriodoIdentificador(a.getPeriodoAquisitivo().getIdentificador());
            dto.setCorHex(a.getPeriodoAquisitivo().getCorHex());
        } else {
            dto.setCorHex(a.getTipoAfastamento().getCorPadrao());
        }

        dto.setTipoAfastamento(a.getTipoAfastamento());
        dto.setTipoDescricao(a.getTipoAfastamento().getDescricao());
        dto.setDataInicio(a.getDataInicio());
        dto.setDataFim(a.getDataFim());
        dto.setDias(a.getDias());
        dto.setFracao(a.getFracao());
        dto.setStatus(a.getStatus());
        var conflitosAtuais = a.getStatus() == StatusFerias.CANCELADO ? List.<AgendamentoFerias>of() :
                agendamentoRepository.findConflitos(a.getServidor().getId(), a.getDataInicio(), a.getDataFim(),
                a.getServidor().getSecretaria() == null ? null : a.getServidor().getSecretaria().getId(), a.getServidor().getSetor(), a.getId());
        dto.setAlertaConflito(!conflitosAtuais.isEmpty());
        dto.setDescricaoConflito(conflitosAtuais.isEmpty() ? null : "Sobreposição com: " + conflitosAtuais.stream()
                .map(c -> c.getServidor().getNome()).distinct().collect(Collectors.joining(", ")));
        dto.setObservacao(a.getObservacao());
        dto.setCriadoEm(a.getCriadoEm());
        dto.setAtualizadoEm(a.getAtualizadoEm());
        return dto;
    }
}