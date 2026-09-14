package com.contract_management.api.service;

import com.contract_management.api.dto.request.EquipeContratoRequestDTO;
import com.contract_management.api.dto.request.MembroEquipeRequestDTO;
import com.contract_management.api.dto.response.EquipeContratoResponseDTO;
import com.contract_management.api.dto.response.MembroEquipeResponseDTO;
import com.contract_management.api.exception.BusinessException;
import com.contract_management.api.exception.EntityNotFoundException;
import com.contract_management.api.model.*;
import com.contract_management.api.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class EquipeContratoService {

    private final EquipeContratoRepository equipeContratoRepository;
    private final AtaRepository ataRepository;
    private final ContratoRepository contratoRepository;
    private final AtivoRepository ativoRepository;
    private final ServidorRepository servidorRepository;
    private final FuncaoEquipeRepository funcaoEquipeRepository;

    @Transactional(readOnly = true)
    public List<EquipeContratoResponseDTO> buscarTodas() {
        List<EquipeContrato> equipes = equipeContratoRepository.findAll();
        return equipes.stream()
                .map(this::converterParaResponseDTO)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public EquipeContratoResponseDTO buscarPorId(Long id) {
        EquipeContrato equipe = equipeContratoRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Equipe", id));
        return converterParaResponseDTO(equipe);
    }

    @Transactional
    public EquipeContratoResponseDTO salvar(EquipeContratoRequestDTO dto) {
        Ativo ativo = ativoRepository.findById(dto.getAtivoId())
                .orElseThrow(() -> new EntityNotFoundException("Ativo", dto.getAtivoId()));

        EquipeContrato equipe = EquipeContrato.builder()
                .ativo(ativo)
                .membros(new ArrayList<>())
                .build();

        vincularOrigem(equipe, dto);
        adicionarMembros(equipe, dto);

        EquipeContrato equipeSalva = equipeContratoRepository.save(equipe);
        return converterParaResponseDTO(equipeSalva);
    }

    @Transactional
    public EquipeContratoResponseDTO atualizar(Long id, EquipeContratoRequestDTO dto) {
        EquipeContrato equipeExistente = equipeContratoRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Equipe", id));

        Ativo ativo = ativoRepository.findById(dto.getAtivoId())
                .orElseThrow(() -> new EntityNotFoundException("Ativo", dto.getAtivoId()));

        equipeExistente.setAtivo(ativo);
        vincularOrigem(equipeExistente, dto);
        equipeExistente.getMembros().clear();
        adicionarMembros(equipeExistente, dto);

        EquipeContrato equipeAtualizada = equipeContratoRepository.save(equipeExistente);
        return converterParaResponseDTO(equipeAtualizada);
    }

    @Transactional
    public void deletar(Long id) {
        if (!equipeContratoRepository.existsById(id)) {
            throw new EntityNotFoundException("Equipe", id);
        }
        equipeContratoRepository.deleteById(id);
    }

    // --- MÉTODOS PRIVADOS DE MAPEAMENTO (EXCLUSIVOS DO SERVICE) ---

    private EquipeContratoResponseDTO converterParaResponseDTO(EquipeContrato equipe) {
        if (equipe == null) return null;

        List<MembroEquipeResponseDTO> membrosDTO = new ArrayList<>();
        if (equipe.getMembros() != null) {
            membrosDTO = equipe.getMembros().stream()
                    .map(this::converterMembroParaResponseDTO)
                    .collect(Collectors.toList());
        }

        String situacaoNome = null;
        if (equipe.getAtivo() != null) {
            // Se o atributo da sua entidade Ativo for 'situacao', 'descricao' ou 'nome', ajuste o getter abaixo:
            situacaoNome = equipe.getAtivo().getSituacao();
        }

        return EquipeContratoResponseDTO.builder()
                .id(equipe.getId())
                .ataId(equipe.getAta() != null ? equipe.getAta().getId() : null)
                .ataNumero(equipe.getAta() != null ? equipe.getAta().getNumero() : null)
                .ataAno(equipe.getAta() != null ? equipe.getAta().getAno() : null)
                .ataObjeto(equipe.getAta() != null ? equipe.getAta().getObjeto() : null)
                .contratoId(equipe.getContrato() != null ? equipe.getContrato().getId() : null)
                .contratoNumero(equipe.getContrato() != null ? equipe.getContrato().getNumero() : null)
                .contratoAno(equipe.getContrato() != null ? equipe.getContrato().getAno() : null)
                .contratoObjeto(equipe.getContrato() != null ? equipe.getContrato().getObjeto() : null)
                .ativoId(equipe.getAtivo() != null ? equipe.getAtivo().getId() : null)
                .situacao(situacaoNome)
                .membros(membrosDTO)
                .build();
    }

    private void vincularOrigem(EquipeContrato equipe, EquipeContratoRequestDTO dto) {
        boolean temAta = dto.getAtaId() != null;
        boolean temContrato = dto.getContratoId() != null;

        if (temAta == temContrato) {
            throw new BusinessException("Informe a ATA ou o Contrato para vincular a equipe");
        }

        if (temAta) {
            AtaRegistroPreco ata = ataRepository.findById(dto.getAtaId())
                    .orElseThrow(() -> new EntityNotFoundException("ATA", dto.getAtaId()));
            equipe.setAta(ata);
            equipe.setContrato(null);
            return;
        }

        Contrato contrato = contratoRepository.findById(dto.getContratoId())
                .orElseThrow(() -> new EntityNotFoundException("Contrato", dto.getContratoId()));
        equipe.setContrato(contrato);
        equipe.setAta(null);
    }

    private void adicionarMembros(EquipeContrato equipe, EquipeContratoRequestDTO dto) {
        if (dto.getMembros() == null) {
            return;
        }
        for (MembroEquipeRequestDTO membroDTO : dto.getMembros()) {
            Servidor servidor = servidorRepository.findById(membroDTO.getServidorId())
                    .orElseThrow(() -> new EntityNotFoundException("Servidor", membroDTO.getServidorId()));

            FuncaoEquipe funcao = funcaoEquipeRepository.findById(membroDTO.getFuncaoId())
                    .orElseThrow(() -> new EntityNotFoundException("Função", membroDTO.getFuncaoId()));

            EquipeMembro membro = EquipeMembro.builder()
                    .equipe(equipe)
                    .servidor(servidor)
                    .funcao(funcao)
                    .build();

            equipe.getMembros().add(membro);
        }
    }

    private MembroEquipeResponseDTO converterMembroParaResponseDTO(EquipeMembro membro) {
        if (membro == null) return null;

        Servidor servidor = membro.getServidor();
        FuncaoEquipe funcao = membro.getFuncao();

        String matriculaStr = null;
        if (servidor != null && servidor.getMatricula() != null) {
            matriculaStr = String.valueOf(servidor.getMatricula()); // Converte Integer para String
        }

        String funcaoNome = null;
        if (funcao != null) {
            funcaoNome = funcao.getNome(); // ou getDescricao() dependendo do atributo em FuncaoEquipe
        }

        return MembroEquipeResponseDTO.builder()
                .id(membro.getId())
                .servidorId(servidor != null ? servidor.getId() : null)
                .servidorNome(servidor != null ? servidor.getNome() : null)
                .servidorCargo(servidor != null ? servidor.getCargo() : null)
                .servidorMatricula(matriculaStr)
                .funcaoId(funcao != null ? funcao.getId() : null)
                .funcaoNome(funcaoNome)
                .build();
    }

}
