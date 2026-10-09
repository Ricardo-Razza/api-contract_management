package com.contract_management.api.modules.contrato.service;

import com.contract_management.api.modules.contrato.dto.request.DocumentoFiltro;
import com.contract_management.api.modules.contrato.dto.response.DocumentoFiltrosDTO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import com.contract_management.api.common.exception.BusinessException;
import com.contract_management.api.common.exception.EntityNotFoundException;
import com.contract_management.api.modules.ativo.model.Ativo;
import com.contract_management.api.modules.ativo.api.AtivoConsulta;
import com.contract_management.api.modules.contrato.dto.request.ContratoRequestDTO;
import com.contract_management.api.modules.contrato.dto.response.ContratoResponseDTO;
import com.contract_management.api.modules.contrato.model.Contrato;
import com.contract_management.api.modules.contrato.model.ContratoSecretaria;
import com.contract_management.api.modules.contrato.model.Tipo;
import com.contract_management.api.modules.contrato.repository.ContratoRepository;
import com.contract_management.api.modules.contrato.repository.ContratoSecretariaRepository;
import com.contract_management.api.modules.contrato.repository.TipoRepository;
import com.contract_management.api.modules.equipe.dto.request.MembroEquipeRequestDTO;
import com.contract_management.api.modules.equipe.dto.response.EquipeContratoResponseDTO;
import com.contract_management.api.modules.contrato.mapper.ContratoMapper;
import com.contract_management.api.modules.equipe.dto.response.MembroEquipeResponseDTO;
import com.contract_management.api.modules.equipe.model.EquipeContrato;
import com.contract_management.api.modules.equipe.model.EquipeMembro;
import com.contract_management.api.modules.equipe.model.FuncaoEquipe;
import com.contract_management.api.modules.equipe.api.EquipesVinculadas;
import com.contract_management.api.modules.equipe.api.FuncaoEquipeConsulta;
import com.contract_management.api.modules.secretaria.dto.response.SecretariaResponseDTO;
import com.contract_management.api.modules.secretaria.model.Secretaria;
import com.contract_management.api.modules.secretaria.api.SecretariaConsulta;
import com.contract_management.api.modules.servidor.model.Servidor;
import com.contract_management.api.modules.servidor.api.ServidorConsulta;

@Service
@RequiredArgsConstructor
@Slf4j
public class ContratoService {

    private final ContratoRepository contratoRepository;
    private final ContratoSecretariaRepository contratoSecretariaRepository;
    private final TipoRepository tipoRepository;
    private final AtivoConsulta ativoRepository;
    private final SecretariaConsulta secretariaRepository;
    private final EquipesVinculadas equipeContratoRepository;
    private final ServidorConsulta servidorRepository;
    private final FuncaoEquipeConsulta funcaoEquipeRepository;
    private final ContratoMapper contratoMapper;

    @Transactional(readOnly = true)
    public List<ContratoResponseDTO> listarTodos() {
        List<Contrato> contratos = contratoRepository.findAllComSecretarias();
        if (!contratos.isEmpty()) {
            contratoRepository.carregarEquipes(contratos);
            boolean temEquipes = contratos.stream().anyMatch(c -> c.getEquipe() != null && !c.getEquipe().isEmpty());
            if (temEquipes) {
                contratoRepository.carregarMembrosEquipes(contratos);
            }
        }
        return contratos.stream()
                .map(contratoMapper::toResponseDTO)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public DocumentoFiltrosDTO filtrosDisponiveis() {
        return new DocumentoFiltrosDTO(contratoRepository.listarAnos(), contratoRepository.listarTipos());
    }

    @Transactional(readOnly = true)
    public Page<ContratoResponseDTO> listarPaginado(Pageable pageable, DocumentoFiltro filtro) {
        // A listagem padrão continua usando paginação do banco. Busca e ordenação especial
        // preservam a semântica anterior sobre DTOs; apenas a página retorna ao navegador.
        boolean ordemPadrao = pageable.getSort().stream().allMatch(order -> order.getProperty().equals("id"));
        if (filtro.vazio() && ordemPadrao) return listarPaginado(pageable);
        return DocumentoListagem.contratos(listarTodos(), pageable, filtro);
    }

    @Transactional(readOnly = true)
    public Page<ContratoResponseDTO> listarPaginado(Pageable pageable) {
        Page<Contrato> pagina = contratoRepository.findAll(pageable);
        if (pagina.hasContent()) {
            List<Contrato> contratos = pagina.getContent();
            contratoRepository.carregarSecretarias(contratos);
            contratoRepository.carregarEquipes(contratos);
            boolean temEquipes = contratos.stream().anyMatch(c -> c.getEquipe() != null && !c.getEquipe().isEmpty());
            if (temEquipes) {
                contratoRepository.carregarMembrosEquipes(contratos);
            }
        }
        return pagina.map(contratoMapper::toResponseDTO);
    }

    @Transactional(readOnly = true)
    public ContratoResponseDTO buscarPorId(Long id) {
        Contrato contrato = contratoRepository.findComSecretariasById(id)
                .orElseThrow(() -> new EntityNotFoundException("Contrato", id));
        contratoRepository.findComEquipeById(id);
        if (contrato.getEquipe() != null && !contrato.getEquipe().isEmpty()) {
            contratoRepository.carregarMembrosEquipePorContratoId(id);
        }
        return contratoMapper.toResponseDTO(contrato);
    }

    @Transactional
    public ContratoResponseDTO criar(ContratoRequestDTO dto) {
        if (contratoRepository.existsByNumeroAndAno(dto.getNumero(), dto.getAno())) {
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
        salvarOuAtualizarEquipe(saved, dto.getMembros(), ativo);

        log.info("Contrato criado com sucesso. ID: {}, Número: {}/{}", saved.getId(), saved.getNumero(), saved.getAno());
        return contratoMapper.toResponseDTO(saved);
    }

    @Transactional
    public ContratoResponseDTO atualizar(Long id, ContratoRequestDTO dto) {
        Contrato contrato = contratoRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Contrato", id));

        if (!contrato.getNumero().equals(dto.getNumero()) || !contrato.getAno().equals(dto.getAno())) {
            if (contratoRepository.existsByNumeroAndAno(dto.getNumero(), dto.getAno())) {
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
        salvarOuAtualizarEquipe(contrato, dto.getMembros(), ativo);

        Contrato atualizado = contratoRepository.save(contrato);
        log.info("Contrato atualizado com sucesso. ID: {}, Número: {}/{}", atualizado.getId(), atualizado.getNumero(), atualizado.getAno());
        return contratoMapper.toResponseDTO(atualizado);
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

    private void salvarOuAtualizarEquipe(Contrato contrato, List<MembroEquipeRequestDTO> membrosDTO, Ativo ativo) {
        if (membrosDTO == null) {
            return;
        }
        List<EquipeContrato> equipesExistentes = equipeContratoRepository.findByContratoId(contrato.getId());
        if (membrosDTO.isEmpty()) {
            if (!equipesExistentes.isEmpty()) {
                equipeContratoRepository.deleteAll(equipesExistentes);
                equipeContratoRepository.flush();
                if (contrato.getEquipe() != null) {
                    contrato.getEquipe().clear();
                }
            }
            return;
        }

        EquipeContrato equipe;
        if (!equipesExistentes.isEmpty()) {
            equipe = equipesExistentes.get(0);
            equipe.setAtivo(ativo);
            if (equipe.getMembros() == null) {
                equipe.setMembros(new ArrayList<>());
            } else {
                equipe.getMembros().clear();
            }
        } else {
            equipe = EquipeContrato.builder()
                    .contrato(contrato)
                    .ativo(ativo)
                    .membros(new ArrayList<>())
                    .build();
        }

        for (MembroEquipeRequestDTO mDto : membrosDTO) {
            Servidor servidor = servidorRepository.findById(mDto.getServidorId())
                    .orElseThrow(() -> new EntityNotFoundException("Servidor", mDto.getServidorId()));
            FuncaoEquipe funcao = funcaoEquipeRepository.findById(mDto.getFuncaoId())
                    .orElseThrow(() -> new EntityNotFoundException("Função", mDto.getFuncaoId()));

            EquipeMembro membro = EquipeMembro.builder()
                    .equipe(equipe)
                    .servidor(servidor)
                    .funcao(funcao)
                    .build();

            equipe.getMembros().add(membro);
        }

        EquipeContrato equipeSalva = equipeContratoRepository.save(equipe);
        if (contrato.getEquipe() == null) {
            contrato.setEquipe(new ArrayList<>());
        }
        contrato.getEquipe().clear();
        contrato.getEquipe().add(equipeSalva);
    }

    private void vincularSecretarias(Contrato contrato, List<Long> secretariasIds, Ativo ativo) {
        if (secretariasIds == null || secretariasIds.isEmpty()) {
            return;
        }
        List<Secretaria> secretarias = secretariaRepository.findAllById(secretariasIds);
        if (secretarias.size() != secretariasIds.size()) {
            for (Long secretariaId : secretariasIds) {
                if (secretarias.stream().noneMatch(s -> s.getId().equals(secretariaId))) {
                    throw new EntityNotFoundException("Secretaria", secretariaId);
                }
            }
        }
        List<ContratoSecretaria> vinculos = secretarias.stream()
                .map(sec -> ContratoSecretaria.builder()
                        .contrato(contrato)
                        .secretaria(sec)
                        .ativo(ativo)
                        .build())
                .collect(Collectors.toList());

        contratoSecretariaRepository.saveAll(vinculos);
    }

    private void validarDatas(ContratoRequestDTO dto) {
        if (dto.getDataFim() != null && dto.getDataFim().isBefore(dto.getDataInicio())) {
            throw new BusinessException("Data fim não pode ser anterior à data início");
        }
    }
}
