package com.contract_management.api.modules.contrato.service;

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
import com.contract_management.api.common.model.Ativo;
import com.contract_management.api.common.repository.AtivoRepository;
import com.contract_management.api.modules.contrato.dto.request.AtaRequestDTO;
import com.contract_management.api.modules.contrato.dto.response.AtaResponseDTO;
import com.contract_management.api.modules.contrato.model.AtaRegistroPreco;
import com.contract_management.api.modules.contrato.model.AtaSecretaria;
import com.contract_management.api.modules.contrato.model.Tipo;
import com.contract_management.api.modules.contrato.repository.AtaRepository;
import com.contract_management.api.modules.contrato.repository.AtaSecretariaRepository;
import com.contract_management.api.modules.contrato.repository.TipoRepository;
import com.contract_management.api.modules.equipe.dto.request.MembroEquipeRequestDTO;
import com.contract_management.api.modules.equipe.dto.response.EquipeContratoResponseDTO;
import com.contract_management.api.modules.equipe.dto.response.MembroEquipeResponseDTO;
import com.contract_management.api.modules.equipe.model.EquipeContrato;
import com.contract_management.api.modules.equipe.model.EquipeMembro;
import com.contract_management.api.modules.equipe.model.FuncaoEquipe;
import com.contract_management.api.modules.equipe.repository.EquipeContratoRepository;
import com.contract_management.api.modules.equipe.repository.FuncaoEquipeRepository;
import com.contract_management.api.modules.secretaria.dto.response.SecretariaResponseDTO;
import com.contract_management.api.modules.secretaria.model.Secretaria;
import com.contract_management.api.modules.secretaria.repository.SecretariaRepository;
import com.contract_management.api.modules.servidor.model.Servidor;
import com.contract_management.api.modules.servidor.repository.ServidorRepository;

@Service
@RequiredArgsConstructor
@Slf4j
public class AtaService {

    private final AtaRepository ataRepository;
    private final AtaSecretariaRepository ataSecretariaRepository;
    private final TipoRepository tipoRepository;
    private final AtivoRepository ativoRepository;
    private final SecretariaRepository secretariaRepository;
    private final EquipeContratoRepository equipeContratoRepository;
    private final ServidorRepository servidorRepository;
    private final FuncaoEquipeRepository funcaoEquipeRepository;

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
                .map(this::toResponseDTO)
                .collect(Collectors.toList());
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
        return pagina.map(this::toResponseDTO);
    }

    @Transactional(readOnly = true)
    public AtaResponseDTO buscarPorId(Long id) {
        AtaRegistroPreco ata = ataRepository.findComSecretariasById(id)
                .orElseThrow(() -> new EntityNotFoundException("ATA", id));
        ataRepository.findComEquipeById(id);
        if (ata.getEquipe() != null && !ata.getEquipe().isEmpty()) {
            ataRepository.carregarMembrosEquipePorAtaId(id);
        }
        return toResponseDTO(ata);
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

    // converte entidade para response
    private AtaResponseDTO toResponseDTO(AtaRegistroPreco ata) {
        AtaResponseDTO dto = new AtaResponseDTO();

        // dados basicos
        dto.setId(ata.getId());
        dto.setNumero(ata.getNumero());
        dto.setAno(ata.getAno());
        dto.setDataInicio(ata.getDataInicio());
        dto.setDataFim(ata.getDataFim());
        dto.setTipo(ata.getTipo() != null ? ata.getTipo().getTipoArp() : null);
        dto.setObjeto(ata.getObjeto());
        dto.setObservacao(ata.getObservacao());
        dto.setSituacao(ata.getAtivo() != null ? ata.getAtivo().getSituacao() : null);
        dto.setPortariaDesignacao(ata.getPortariaDesignacao());
        dto.setDataDesignacao(ata.getDataDesignacao());

        // relacionamentos
        dto.setSecretarias(mapSecretarias(ata.getSecretarias()));
        dto.setEquipe(extractEquipe(ata));

        return dto;
    }

    // mapeia secretarias
    private List<SecretariaResponseDTO> mapSecretarias(List<AtaSecretaria> secretarias) {
        if (secretarias == null || secretarias.isEmpty()) {
            return new ArrayList<>();
        }
        return secretarias.stream()
                .map(this::toSecretariaResponseDTO)
                .collect(Collectors.toList());
    }

    private SecretariaResponseDTO toSecretariaResponseDTO(AtaSecretaria as) {
        SecretariaResponseDTO dto = new SecretariaResponseDTO();
        dto.setId(as.getSecretaria().getId());
        dto.setNome(as.getSecretaria().getNome());
        dto.setSigla(as.getSecretaria().getSigla());
        dto.setSituacao(as.getAtivo() != null ? as.getAtivo().getSituacao() : null);
        return dto;
    }

    // extrai equipes do contrato
    private List<EquipeContratoResponseDTO> extractEquipe(AtaRegistroPreco ata) {
        if (ata.getEquipe() == null || ata.getEquipe().isEmpty()) {
            return new ArrayList<>();
        }
        return ata.getEquipe().stream()
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
        dto.setAtaId(equipe.getAta() != null ? equipe.getAta().getId() : null);
        dto.setAtaNumero(equipe.getAta() != null ? equipe.getAta().getNumero() : null);
        dto.setAtaAno(equipe.getAta() != null ? equipe.getAta().getAno() : null);
        dto.setAtaObjeto(equipe.getAta() != null ? equipe.getAta().getObjeto() : null);
        dto.setAtivoId(equipe.getAtivo() != null ? equipe.getAtivo().getId() : null);
        dto.setSituacao(equipe.getAtivo() != null ? equipe.getAtivo().getSituacao() : null);
        dto.setMembros(membrosDTO);

        return dto;
    }

    private MembroEquipeResponseDTO toMembroResponseDTO(EquipeMembro membro) {
        if (membro == null) return null;

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
            dto.setFuncaoNome(membro.getFuncao().getNome()); // ou getDescricao()
        }

        return dto;
    }
}