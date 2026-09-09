package com.contract_management.api.service;

import com.contract_management.api.dto.request.ContratoRequestDTO;
import com.contract_management.api.dto.response.ContratoResponseDTO;
import com.contract_management.api.dto.response.EquipeContratoResponseDTO;
import com.contract_management.api.dto.response.MembroEquipeResponseDTO;
import com.contract_management.api.dto.response.SecretariaResponseDTO;
import com.contract_management.api.exception.BusinessException;
import com.contract_management.api.exception.EntityNotFoundException;
import com.contract_management.api.model.*;
import com.contract_management.api.repository.AtivoRepository;
import com.contract_management.api.repository.ContratoRepository;
import com.contract_management.api.repository.ContratoSecretariaRepository;
import com.contract_management.api.repository.SecretariaRepository;
import com.contract_management.api.repository.TipoRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class ContratoService {

    private final ContratoRepository contratoRepository;
    private final ContratoSecretariaRepository contratoSecretariaRepository;
    private final TipoRepository tipoRepository;
    private final AtivoRepository ativoRepository;
    private final SecretariaRepository secretariaRepository;

    @Transactional(readOnly = true)
    public List<ContratoResponseDTO> listarTodos() {
        return contratoRepository.findAll().stream()
                .map(this::toResponseDTO)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public ContratoResponseDTO buscarPorId(Long id) {
        Contrato contrato = contratoRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Contrato", id));
        return toResponseDTO(contrato);
    }

    @Transactional
    public ContratoResponseDTO criar(ContratoRequestDTO dto) {
        if (contratoRepository.findByNumeroAndAno(dto.getNumero(), dto.getAno()).isPresent()) {
            throw new BusinessException("Contrato " + dto.getNumero() + "/" + dto.getAno() + " já existe");
        }

        validarDatas(dto);

        Tipo tipo = tipoRepository.findById(dto.getTipoId())
                .orElseThrow(() -> new EntityNotFoundException("Tipo", dto.getTipoId()));

        Ativo ativo = ativoRepository.findById(dto.getAtivoId())
                .orElseThrow(() -> new EntityNotFoundException("Ativo", dto.getAtivoId()));

        Contrato contrato = Contrato.builder()
                .numero(dto.getNumero())
                .ano(dto.getAno())
                .dataInicio(dto.getDataInicio())
                .dataFim(dto.getDataFim())
                .tipo(tipo)
                .objeto(dto.getObjeto())
                .nomeContratado(dto.getNomeContratado())
                .portariaDesignacao(dto.getPortariaDesignacao())
                .dataDesignacao(dto.getDataDesignacao())
                .ativo(ativo)
                .observacao(dto.getObservacao())
                .build();

        Contrato saved = contratoRepository.save(contrato);
        vincularSecretarias(saved, dto.getSecretariasIds(), ativo);

        log.info("Contrato criado com sucesso. ID: {}, Número: {}/{}", saved.getId(), saved.getNumero(), saved.getAno());
        return toResponseDTO(saved);
    }

    @Transactional
    public ContratoResponseDTO atualizar(Long id, ContratoRequestDTO dto) {
        Contrato contrato = contratoRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Contrato", id));

        if (!contrato.getNumero().equals(dto.getNumero()) || !contrato.getAno().equals(dto.getAno())) {
            if (contratoRepository.findByNumeroAndAno(dto.getNumero(), dto.getAno()).isPresent()) {
                throw new BusinessException("Contrato " + dto.getNumero() + "/" + dto.getAno() + " já existe");
            }
        }

        validarDatas(dto);

        Tipo tipo = tipoRepository.findById(dto.getTipoId())
                .orElseThrow(() -> new EntityNotFoundException("Tipo", dto.getTipoId()));

        Ativo ativo = ativoRepository.findById(dto.getAtivoId())
                .orElseThrow(() -> new EntityNotFoundException("Ativo", dto.getAtivoId()));

        contrato.setNumero(dto.getNumero());
        contrato.setAno(dto.getAno());
        contrato.setDataInicio(dto.getDataInicio());
        contrato.setDataFim(dto.getDataFim());
        contrato.setTipo(tipo);
        contrato.setObjeto(dto.getObjeto());
        contrato.setNomeContratado(dto.getNomeContratado());
        contrato.setPortariaDesignacao(dto.getPortariaDesignacao());
        contrato.setDataDesignacao(dto.getDataDesignacao());
        contrato.setObservacao(dto.getObservacao());
        contrato.setAtivo(ativo);

        List<ContratoSecretaria> antigas = contratoSecretariaRepository.findByContratoId(id);
        if (!antigas.isEmpty()) {
            contratoSecretariaRepository.deleteAll(antigas);
            contratoSecretariaRepository.flush();
        }

        vincularSecretarias(contrato, dto.getSecretariasIds(), ativo);

        Contrato atualizado = contratoRepository.save(contrato);
        log.info("Contrato atualizado com sucesso. ID: {}, Número: {}/{}", atualizado.getId(), atualizado.getNumero(), atualizado.getAno());
        return toResponseDTO(atualizado);
    }

    @Transactional
    public void deletar(Long id) {
        if (!contratoRepository.existsById(id)) {
            throw new EntityNotFoundException("Contrato", id);
        }
        contratoSecretariaRepository.deleteByContratoId(id);
        contratoRepository.deleteById(id);
        log.info("Contrato deletado com sucesso. ID: {}", id);
    }

    private void vincularSecretarias(Contrato contrato, List<Long> secretariasIds, Ativo ativo) {
        if (secretariasIds == null || secretariasIds.isEmpty()) {
            return;
        }
        for (Long secretariaId : secretariasIds) {
            Secretaria secretaria = secretariaRepository.findById(secretariaId)
                    .orElseThrow(() -> new EntityNotFoundException("Secretaria", secretariaId));

            ContratoSecretaria contratoSecretaria = ContratoSecretaria.builder()
                    .contrato(contrato)
                    .secretaria(secretaria)
                    .ativo(ativo)
                    .build();

            contratoSecretariaRepository.save(contratoSecretaria);
        }
    }

    private void validarDatas(ContratoRequestDTO dto) {
        if (dto.getDataFim() != null && dto.getDataFim().isBefore(dto.getDataInicio())) {
            throw new BusinessException("Data fim não pode ser anterior à data início");
        }
    }

    private ContratoResponseDTO toResponseDTO(Contrato contrato) {
        ContratoResponseDTO dto = new ContratoResponseDTO();
        dto.setId(contrato.getId());
        dto.setNumero(contrato.getNumero());
        dto.setAno(contrato.getAno());
        dto.setDataInicio(contrato.getDataInicio());
        dto.setDataFim(contrato.getDataFim());
        dto.setTipo(contrato.getTipo() != null ? contrato.getTipo().getTipoArp() : null);
        dto.setObjeto(contrato.getObjeto());
        dto.setNomeContratado(contrato.getNomeContratado());
        dto.setPortariaDesignacao(contrato.getPortariaDesignacao());
        dto.setDataDesignacao(contrato.getDataDesignacao());
        dto.setSituacao(contrato.getAtivo() != null ? contrato.getAtivo().getSituacao() : null);
        dto.setObservacao(contrato.getObservacao());
        dto.setSecretarias(mapSecretarias(contratoSecretariaRepository.findByContratoId(contrato.getId())));
        dto.setEquipe(extractEquipe(contrato));
        return dto;
    }

    private List<SecretariaResponseDTO> mapSecretarias(List<ContratoSecretaria> secretarias) {
        if (secretarias == null || secretarias.isEmpty()) {
            return new ArrayList<>();
        }
        return secretarias.stream()
                .map(this::toSecretariaResponseDTO)
                .collect(Collectors.toList());
    }

    private SecretariaResponseDTO toSecretariaResponseDTO(ContratoSecretaria cs) {
        SecretariaResponseDTO dto = new SecretariaResponseDTO();
        dto.setId(cs.getSecretaria().getId());
        dto.setNome(cs.getSecretaria().getNome());
        dto.setSigla(cs.getSecretaria().getSigla());
        dto.setSituacao(cs.getAtivo() != null ? cs.getAtivo().getSituacao() : null);
        return dto;
    }

    private List<EquipeContratoResponseDTO> extractEquipe(Contrato contrato) {
        if (contrato.getEquipe() == null || contrato.getEquipe().isEmpty()) {
            return new ArrayList<>();
        }
        return contrato.getEquipe().stream()
                .map(this::toEquipeResponseDTO)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }

    private EquipeContratoResponseDTO toEquipeResponseDTO(EquipeContrato equipe) {
        if (equipe == null) {
            return null;
        }

        List<MembroEquipeResponseDTO> membrosDTO = new ArrayList<>();
        if (equipe.getMembros() != null) {
            membrosDTO = equipe.getMembros().stream()
                    .map(this::toMembroResponseDTO)
                    .collect(Collectors.toList());
        }

        EquipeContratoResponseDTO dto = new EquipeContratoResponseDTO();
        dto.setId(equipe.getId());
        dto.setContratoId(equipe.getContrato() != null ? equipe.getContrato().getId() : null);
        dto.setContratoNumero(equipe.getContrato() != null ? equipe.getContrato().getNumero() : null);
        dto.setContratoAno(equipe.getContrato() != null ? equipe.getContrato().getAno() : null);
        dto.setContratoObjeto(equipe.getContrato() != null ? equipe.getContrato().getObjeto() : null);
        dto.setAtivoId(equipe.getAtivo() != null ? equipe.getAtivo().getId() : null);
        dto.setSituacao(equipe.getAtivo() != null ? equipe.getAtivo().getSituacao() : null);
        dto.setMembros(membrosDTO);
        return dto;
    }

    private MembroEquipeResponseDTO toMembroResponseDTO(EquipeMembro membro) {
        if (membro == null) {
            return null;
        }

        MembroEquipeResponseDTO dto = new MembroEquipeResponseDTO();
        dto.setId(membro.getId());

        if (membro.getServidor() != null) {
            dto.setServidorId(membro.getServidor().getId());
            dto.setServidorNome(membro.getServidor().getNome());
            dto.setServidorCargo(membro.getServidor().getCargo());
            if (membro.getServidor().getMatricula() != null) {
                dto.setServidorMatricula(String.valueOf(membro.getServidor().getMatricula()));
            }
        }

        if (membro.getFuncao() != null) {
            dto.setFuncaoId(membro.getFuncao().getId());
            dto.setFuncaoNome(membro.getFuncao().getNome());
        }

        return dto;
    }
}
