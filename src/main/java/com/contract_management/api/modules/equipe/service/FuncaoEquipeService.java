package com.contract_management.api.modules.equipe.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;
import java.util.stream.Collectors;

import com.contract_management.api.common.exception.EntityNotFoundException;
import com.contract_management.api.modules.equipe.dto.response.FuncaoEquipeResponseDTO;
import com.contract_management.api.modules.equipe.model.FuncaoEquipe;
import com.contract_management.api.modules.equipe.repository.FuncaoEquipeRepository;

@Service
@RequiredArgsConstructor
@Slf4j
public class FuncaoEquipeService {

    private final FuncaoEquipeRepository funcaoEquipeRepository;

    @Transactional(readOnly = true)
    public List<FuncaoEquipeResponseDTO> listarTodos() {
        log.info("Buscando todas as funções de equipe");
        return funcaoEquipeRepository.findAll().stream()
                .map(this::toResponseDTO)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public FuncaoEquipeResponseDTO buscarPorId(Long id) {
        log.info("Buscando função com ID: {}", id);
        FuncaoEquipe funcao = funcaoEquipeRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("FuncaoEquipe", id));
        return toResponseDTO(funcao);
    }

    private FuncaoEquipeResponseDTO toResponseDTO(FuncaoEquipe funcao) {
        FuncaoEquipeResponseDTO dto = new FuncaoEquipeResponseDTO();
        dto.setId(funcao.getId());
        dto.setNome(funcao.getNome());
        dto.setSituacao(funcao.getAtivo().getSituacao());
        return dto;
    }
}