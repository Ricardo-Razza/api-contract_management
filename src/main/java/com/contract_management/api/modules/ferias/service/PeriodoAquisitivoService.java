package com.contract_management.api.modules.ferias.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

import com.contract_management.api.common.exception.BusinessException;
import com.contract_management.api.common.exception.EntityNotFoundException;
import com.contract_management.api.modules.ferias.dto.request.PeriodoAquisitivoRequestDTO;
import com.contract_management.api.modules.ferias.dto.response.PeriodoAquisitivoResponseDTO;
import com.contract_management.api.modules.ferias.model.PeriodoAquisitivo;
import com.contract_management.api.modules.ferias.model.StatusFerias;
import com.contract_management.api.modules.ferias.repository.AgendamentoFeriasRepository;
import com.contract_management.api.modules.ferias.repository.PeriodoAquisitivoRepository;
import com.contract_management.api.modules.servidor.model.Servidor;
import com.contract_management.api.modules.servidor.api.ServidorConsulta;

@Service
@RequiredArgsConstructor
@Slf4j
public class PeriodoAquisitivoService {

    private final PeriodoAquisitivoRepository periodoRepository;
    private final ServidorConsulta servidorRepository;
    private final jakarta.persistence.EntityManager entityManager;
    private final AgendamentoFeriasRepository agendamentoRepository;

    @Transactional(readOnly = true)
    public List<PeriodoAquisitivoResponseDTO> listar() {
        return periodoRepository.listarTodos().stream().map(this::toResponseDTO).toList();
    }

    private void validar(PeriodoAquisitivoRequestDTO dto, Long id) {
        if (dto.getDataFim().isBefore(dto.getDataInicio())) throw new BusinessException("Data final anterior à inicial.");
        if (dto.getAnoFim() < dto.getAnoInicio()) throw new BusinessException("Ano final anterior ao inicial.");
        if (dto.getTotalDias() == null || dto.getTotalDias() < 1 || dto.getTotalDias() > 60)
            throw new BusinessException("O total deve estar entre 1 e 60 dias.");
        if (dto.getLimiteGozo() != null && dto.getLimiteGozo().isBefore(dto.getDataFim()))
            throw new BusinessException("Limite de gozo anterior ao fim do período aquisitivo.");
        boolean repetido = periodoRepository.findByServidorIdOrderByAnoInicioDesc(dto.getServidorId()).stream()
                .anyMatch(p -> !p.getId().equals(id) && !p.getDataInicio().isAfter(dto.getDataFim()) && !p.getDataFim().isBefore(dto.getDataInicio()));
        if (repetido) throw new BusinessException("Já existe um período aquisitivo que abrange essas datas.");
    }

    private static final List<String> PALETA_CORES = List.of(
            "#eab308", // Amarelo (ex: 2025/2026)
            "#7e22ce", // Roxo (ex: 2024/2025)
            "#2563eb", // Azul (ex: 2023/2024)
            "#059669", // Verde (ex: 2022/2023)
            "#84cc16", // Oliva (ex: 2021/2022)
            "#047857", // Verde Escuro (ex: 2020/2021)
            "#ca8a04", // Mostarda (ex: 2019/2020)
            "#1e3a8a", // Azul Marinho (ex: 2018/2019)
            "#ea580c", // Laranja (ex: 2017/2018)
            "#0891b2"  // Ciano (ex: 2016/2017)
    );

    @Transactional(readOnly = true)
    public List<PeriodoAquisitivoResponseDTO> listarPorServidor(Long servidorId) {
        log.info("Listando períodos aquisitivos para o servidor ID: {}", servidorId);
        return periodoRepository.findByServidorIdOrderByAnoInicioDesc(servidorId).stream()
                .map(this::toResponseDTO)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public PeriodoAquisitivoResponseDTO buscarPorId(Long id) {
        log.info("Buscando período aquisitivo ID: {}", id);
        PeriodoAquisitivo periodo = periodoRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Período Aquisitivo", id));
        return toResponseDTO(periodo);
    }

    @Transactional(isolation = org.springframework.transaction.annotation.Isolation.READ_COMMITTED)
    public PeriodoAquisitivoResponseDTO criar(PeriodoAquisitivoRequestDTO dto) {
        log.info("Criando período aquisitivo para servidor ID: {}", dto.getServidorId());

        Servidor servidor = servidorRepository.bloquearPorId(dto.getServidorId())
                .orElseThrow(() -> new EntityNotFoundException("Servidor", dto.getServidorId()));

        validar(dto, null);
        String identificador = dto.getIdentificador();
        if (identificador == null || identificador.isBlank()) {
            identificador = dto.getAnoInicio() + "/" + dto.getAnoFim();
        }

        String corHex = dto.getCorHex();
        if (corHex == null || corHex.isBlank()) {
            corHex = sugerirCorPorAno(dto.getAnoInicio());
        }

        int totalDias = (dto.getTotalDias() != null && dto.getTotalDias() > 0) ? dto.getTotalDias() : 30;

        PeriodoAquisitivo periodo = PeriodoAquisitivo.builder()
                .servidor(servidor)
                .anoInicio(dto.getAnoInicio())
                .anoFim(dto.getAnoFim())
                .identificador(identificador)
                .dataInicio(dto.getDataInicio())
                .dataFim(dto.getDataFim())
                .limiteGozo(dto.getLimiteGozo() != null ? dto.getLimiteGozo() : dto.getDataFim().plusYears(1))
                .totalDias(totalDias)
                .diasUsados(0)
                .diasRestantes(totalDias)
                .corHex(corHex)
                .build();

        PeriodoAquisitivo saved = periodoRepository.save(periodo);
        log.info("Período aquisitivo criado com sucesso. ID: {}", saved.getId());
        return toResponseDTO(saved);
    }

    @Transactional(isolation = org.springframework.transaction.annotation.Isolation.READ_COMMITTED)
    public PeriodoAquisitivoResponseDTO atualizar(Long id, PeriodoAquisitivoRequestDTO dto) {
        log.info("Atualizando período aquisitivo ID: {}", id);

        PeriodoAquisitivo periodo = periodoRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Período Aquisitivo", id));

        if (!periodo.getServidor().getId().equals(dto.getServidorId())) throw new BusinessException("O servidor não pode ser alterado.");
        servidorRepository.bloquearPorId(dto.getServidorId()).orElseThrow();
        periodo = periodoRepository.bloquearPorId(id).orElseThrow();
        entityManager.refresh(periodo);
        validar(dto, id);
        if (dto.getTotalDias() < periodo.getDiasUsados()) throw new BusinessException("O total não pode ser menor que os dias já comprometidos.");
        periodo.setAnoInicio(dto.getAnoInicio());
        periodo.setAnoFim(dto.getAnoFim());
        periodo.setIdentificador(dto.getIdentificador() != null && !dto.getIdentificador().isBlank()
                ? dto.getIdentificador() : dto.getAnoInicio() + "/" + dto.getAnoFim());
        periodo.setDataInicio(dto.getDataInicio());
        periodo.setDataFim(dto.getDataFim());
        periodo.setLimiteGozo(dto.getLimiteGozo());
        if (dto.getCorHex() != null && !dto.getCorHex().isBlank()) {
            periodo.setCorHex(dto.getCorHex());
        }
        if (dto.getTotalDias() != null) {
            periodo.setTotalDias(dto.getTotalDias());
            periodo.setDiasRestantes(Math.max(0, dto.getTotalDias() - periodo.getDiasUsados()));
        }

        PeriodoAquisitivo updated = periodoRepository.save(periodo);
        return toResponseDTO(updated);
    }

    @Transactional(isolation = org.springframework.transaction.annotation.Isolation.READ_COMMITTED)
    public void deletar(Long id) {
        log.info("Deletando período aquisitivo ID: {}", id);
        PeriodoAquisitivo periodo = periodoRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Período Aquisitivo", id));

        servidorRepository.bloquearPorId(periodo.getServidor().getId()).orElseThrow();
        periodo = periodoRepository.bloquearPorId(id).orElseThrow();
        entityManager.refresh(periodo);
        if (!agendamentoRepository.findByPeriodoAquisitivoId(id).isEmpty()) {
            throw new BusinessException("Não é possível excluir um período aquisitivo que já possui férias gozadas ou agendadas.");
        }

        periodoRepository.delete(periodo);
    }

    @Transactional(isolation = org.springframework.transaction.annotation.Isolation.READ_COMMITTED)
    public void debitarDias(PeriodoAquisitivo periodo, int dias) {
        if (periodo.getDiasRestantes() < dias) {
            throw new BusinessException(String.format(
                    "Saldo insuficiente no período aquisitivo %s. Saldo restante: %d dias, solicitado: %d dias.",
                    periodo.getIdentificador(), periodo.getDiasRestantes(), dias));
        }
        periodo.setDiasUsados(periodo.getDiasUsados() + dias);
        periodo.setDiasRestantes(periodo.getTotalDias() - periodo.getDiasUsados());
        periodoRepository.save(periodo);
    }

    @Transactional(isolation = org.springframework.transaction.annotation.Isolation.READ_COMMITTED)
    public void estornarDias(PeriodoAquisitivo periodo, int dias) {
        periodo.setDiasUsados(Math.max(0, periodo.getDiasUsados() - dias));
        periodo.setDiasRestantes(Math.min(periodo.getTotalDias(), periodo.getTotalDias() - periodo.getDiasUsados()));
        periodoRepository.save(periodo);
    }

    public String sugerirCorPorAno(int ano) {
        int index = Math.floorMod(2025 - ano, PALETA_CORES.size());
        return PALETA_CORES.get(index);
    }

    public PeriodoAquisitivoResponseDTO toResponseDTO(PeriodoAquisitivo periodo) {
        PeriodoAquisitivoResponseDTO dto = new PeriodoAquisitivoResponseDTO();
        dto.setId(periodo.getId());
        dto.setServidorId(periodo.getServidor().getId());
        dto.setServidorNome(periodo.getServidor().getNome());
        dto.setServidorMatricula(periodo.getServidor().getMatricula());
        dto.setAnoInicio(periodo.getAnoInicio());
        dto.setAnoFim(periodo.getAnoFim());
        dto.setIdentificador(periodo.getIdentificador());
        dto.setDataInicio(periodo.getDataInicio());
        dto.setDataFim(periodo.getDataFim());
        dto.setLimiteGozo(periodo.getLimiteGozo());
        dto.setTotalDias(periodo.getTotalDias());
        dto.setDiasUsados(periodo.getDiasUsados());
        int gozados = agendamentoRepository.findByPeriodoAquisitivoId(periodo.getId()).stream()
                .filter(a -> a.getStatus() == StatusFerias.CONFIRMADO)
                .mapToInt(a -> {
                    LocalDate ate = a.getDataFim().isBefore(LocalDate.now()) ? a.getDataFim() : LocalDate.now();
                    return ate.isBefore(a.getDataInicio()) ? 0 : (int) java.time.temporal.ChronoUnit.DAYS.between(a.getDataInicio(), ate) + 1;
                }).sum();
        dto.setDiasGozados(gozados);
        dto.setDiasReservados(Math.max(0, periodo.getDiasUsados() - gozados));
        dto.setSecretariaId(periodo.getServidor().getSecretaria() == null ? null : periodo.getServidor().getSecretaria().getId());
        dto.setServidorSetor(periodo.getServidor().getSetor());
        dto.setDiasRestantes(periodo.getDiasRestantes());
        dto.setCorHex(periodo.getCorHex());
        dto.setCriadoEm(periodo.getCriadoEm());
        return dto;
    }
}