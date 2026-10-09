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
import com.contract_management.api.modules.contrato.dto.request.AtaRequestDTO;
import com.contract_management.api.modules.contrato.dto.response.AtaResponseDTO;
import com.contract_management.api.modules.contrato.model.AtaRegistroPreco;
import com.contract_management.api.modules.contrato.model.AtaSecretaria;
import com.contract_management.api.modules.contrato.model.Tipo;
import com.contract_management.api.modules.contrato.repository.AtaRepository;
import com.contract_management.api.modules.contrato.repository.AtaSecretariaRepository;
import com.contract_management.api.modules.contrato.repository.TipoRepository;
import com.contract_management.api.modules.contrato.mapper.AtaMapper;
import com.contract_management.api.modules.equipe.dto.request.MembroEquipeRequestDTO;
import com.contract_management.api.modules.equipe.dto.response.EquipeContratoResponseDTO;
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
public class AtaService {

    private final AtaRepository ataRepository;
    private final AtaSecretariaRepository ataSecretariaRepository;
    private final TipoRepository tipoRepository;
    private final AtivoConsulta ativoRepository;
    private final SecretariaConsulta secretariaRepository;
    private final EquipesVinculadas equipeContratoRepository;
    private final ServidorConsulta servidorRepository;
    private final FuncaoEquipeConsulta funcaoEquipeRepository;
    private final AtaMapper ataMapper;

    @Transactional(readOnly = true)
    public List<AtaResponseDTO> listarTodos() {
        List<AtaRegistroPreco> atas = ataRepository.findAllComSecretarias();
        if (!atas.isEmpty()) {
            ataRepository.carregarEquipes(atas);
            boolean temEquipes = atas.stream().anyMatch(a -> a.getEquipe() != null && !a.getEquipe().isEmpty());
            if (temEquipes) {
                ataRepository.carregarMembrosEquipes(atas);
            }
        }
        return atas.stream()
                .map(ataMapper::toResponseDTO)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public DocumentoFiltrosDTO filtrosDisponiveis() {
        return new DocumentoFiltrosDTO(ataRepository.listarAnos(), ataRepository.listarTipos());
    }

    @Transactional(readOnly = true)
    public Page<AtaResponseDTO> listarPaginado(Pageable pageable, DocumentoFiltro filtro) {
        // A listagem padrão continua usando paginação do banco. Busca e ordenação especial
        // preservam a semântica anterior sobre DTOs; apenas a página retorna ao navegador.
        boolean ordemPadrao = pageable.getSort().stream().allMatch(order -> order.getProperty().equals("id"));
        if (filtro.vazio() && ordemPadrao) return listarPaginado(pageable);
        return DocumentoListagem.atas(listarTodos(), pageable, filtro);
    }

    @Transactional(readOnly = true)
    public Page<AtaResponseDTO> listarPaginado(Pageable pageable) {
        Page<AtaRegistroPreco> pagina = ataRepository.findAll(pageable);
        if (pagina.hasContent()) {
            List<AtaRegistroPreco> atas = pagina.getContent();
            ataRepository.carregarSecretarias(atas);
            ataRepository.carregarEquipes(atas);
            boolean temEquipes = atas.stream().anyMatch(a -> a.getEquipe() != null && !a.getEquipe().isEmpty());
            if (temEquipes) {
                ataRepository.carregarMembrosEquipes(atas);
            }
        }
        return pagina.map(ataMapper::toResponseDTO);
    }

    @Transactional(readOnly = true)
    public AtaResponseDTO buscarPorId(Long id) {
        AtaRegistroPreco ata = ataRepository.findComSecretariasById(id)
                .orElseThrow(() -> new EntityNotFoundException("ATA", id));
        ataRepository.findComEquipeById(id);
        if (ata.getEquipe() != null && !ata.getEquipe().isEmpty()) {
            ataRepository.carregarMembrosEquipePorAtaId(id);
        }
        return ataMapper.toResponseDTO(ata);
    }

    @Transactional
    public AtaResponseDTO criar(AtaRequestDTO dto) {
        // valida duplicidade
        if (ataRepository.findByNumeroAndAno(dto.getNumero(), dto.getAno()).isPresent()) {
            throw new BusinessException("ATA " + dto.getNumero() + "/" + dto.getAno() + " já existe");
        }

        // valida datas
        if (dto.getDataFim() != null && dto.getDataFim().isBefore(dto.getDataInicio())) {
            throw new BusinessException("Data fim não pode ser anterior à data início");
        }

        // busca dependencias
        Tipo tipo = tipoRepository.findById(dto.getTipoId())
                .orElseThrow(() -> new EntityNotFoundException("Tipo", dto.getTipoId()));

        Ativo ativo = ativoRepository.findById(dto.getAtivoId())
                .orElseThrow(() -> new EntityNotFoundException("Ativo", dto.getAtivoId()));

        // constroi a entidade
        AtaRegistroPreco ata = AtaRegistroPreco.builder()
                .numero(dto.getNumero())
                .ano(dto.getAno())
                .dataInicio(dto.getDataInicio())
                .dataFim(dto.getDataFim())
                .tipo(tipo)
                .objeto(dto.getObjeto())
                .observacao(dto.getObservacao())
                .portariaDesignacao(dto.getPortariaDesignacao())
                .dataDesignacao(dto.getDataDesignacao())
                .ativo(ativo)
                .build();

        AtaRegistroPreco saved = ataRepository.save(ata);

        // vincula secretarias
        if (dto.getSecretariasIds() != null && !dto.getSecretariasIds().isEmpty()) {
            for (Long secretariaId : dto.getSecretariasIds()) {
                Secretaria secretaria = secretariaRepository.findById(secretariaId)
                        .orElseThrow(() -> new EntityNotFoundException("Secretaria", secretariaId));

                AtaSecretaria ataSecretaria = AtaSecretaria.builder()
                        .ata(saved)
                        .secretaria(secretaria)
                        .ativo(ativo)
                        .build();

                ataSecretariaRepository.save(ataSecretaria);
            }
        }

        salvarOuAtualizarEquipe(saved, dto.getMembros(), ativo);

        log.info("ATA criada com sucesso. ID: {}, Número: {}/{}", saved.getId(), saved.getNumero(), saved.getAno());
        return buscarPorId(saved.getId());
    }

    @Transactional
    public AtaResponseDTO atualizar(Long id, AtaRequestDTO dto) {
        // 1. Buscar a ata existente
        AtaRegistroPreco ata = ataRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("ATA", id));

        // 2. Validar duplicidade
        if (!ata.getNumero().equals(dto.getNumero()) || !ata.getAno().equals(dto.getAno())) {
            if (ataRepository.findByNumeroAndAno(dto.getNumero(), dto.getAno()).isPresent()) {
                throw new BusinessException("ATA " + dto.getNumero() + "/" + dto.getAno() + " já existe");
            }
        }

        // 3. Validar datas
        if (dto.getDataFim() != null && dto.getDataFim().isBefore(dto.getDataInicio())) {
            throw new BusinessException("Data fim não pode ser anterior à data início");
        }

        // 4. Buscar dependências
        Tipo tipo = tipoRepository.findById(dto.getTipoId())
                .orElseThrow(() -> new EntityNotFoundException("Tipo", dto.getTipoId()));

        Ativo ativo = ativoRepository.findById(dto.getAtivoId())
                .orElseThrow(() -> new EntityNotFoundException("Ativo", dto.getAtivoId()));

        // 5. Atualizar dados
        ata.setNumero(dto.getNumero());
        ata.setAno(dto.getAno());
        ata.setDataInicio(dto.getDataInicio());
        ata.setDataFim(dto.getDataFim());
        ata.setTipo(tipo);
        ata.setObjeto(dto.getObjeto());
        ata.setObservacao(dto.getObservacao());
        ata.setPortariaDesignacao(dto.getPortariaDesignacao());
        ata.setDataDesignacao(dto.getDataDesignacao());
        ata.setAtivo(ativo);

        // 6. ATUALIZAR SECRETARIAS - REMOVER ANTIGAS
        List<AtaSecretaria> antigas = ataSecretariaRepository.findByAtaId(id);
        if (!antigas.isEmpty()) {
            ataSecretariaRepository.deleteAll(antigas);
            ataSecretariaRepository.flush();  // Força a execução imediata
        }

        // 7. ADICIONAR NOVAS SECRETARIAS
        if (dto.getSecretariasIds() != null && !dto.getSecretariasIds().isEmpty()) {
            for (Long secretariaId : dto.getSecretariasIds()) {
                Secretaria secretaria = secretariaRepository.findById(secretariaId)
                        .orElseThrow(() -> new EntityNotFoundException("Secretaria", secretariaId));

                AtaSecretaria ataSecretaria = AtaSecretaria.builder()
                        .ata(ata)
                        .secretaria(secretaria)
                        .ativo(ativo)
                        .build();

                ataSecretariaRepository.save(ataSecretaria);
            }
        }

        salvarOuAtualizarEquipe(ata, dto.getMembros(), ativo);

        AtaRegistroPreco updated = savedOrUpdated(ata);
        log.info("ATA atualizada com sucesso. ID: {}, Número: {}/{}", ata.getId(), ata.getNumero(), ata.getAno());
        return buscarPorId(updated.getId());
    }

    private void salvarOuAtualizarEquipe(AtaRegistroPreco ata, List<MembroEquipeRequestDTO> membrosDTO, Ativo ativo) {
        if (membrosDTO == null) {
            return;
        }
        List<EquipeContrato> equipesExistentes = equipeContratoRepository.findByAtaId(ata.getId());
        if (membrosDTO.isEmpty()) {
            if (!equipesExistentes.isEmpty()) {
                equipeContratoRepository.deleteAll(equipesExistentes);
                equipeContratoRepository.flush();
                if (ata.getEquipe() != null) {
                    ata.getEquipe().clear();
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
                    .ata(ata)
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
        if (ata.getEquipe() == null) {
            ata.setEquipe(new ArrayList<>());
        }
        ata.getEquipe().clear();
        ata.getEquipe().add(equipeSalva);
    }

    @Transactional
    public void deletar(Long id) {
        if (!ataRepository.existsById(id)) {
            throw new EntityNotFoundException("ATA", id);
        }
        ataSecretariaRepository.deleteByAtaId(id);
        ataRepository.deleteById(id);
        log.info("ATA deletada com sucesso. ID: {}", id);
    }

    private AtaRegistroPreco savedOrUpdated(AtaRegistroPreco ata) {
        return ataRepository.save(ata);
    }
}
