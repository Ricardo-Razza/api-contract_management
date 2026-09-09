package com.contract_management.api.service;

import com.contract_management.api.dto.request.SecretariaRequestDTO;
import com.contract_management.api.dto.response.SecretariaResponseDTO;
import com.contract_management.api.exception.BusinessException;
import com.contract_management.api.exception.EntityNotFoundException;
import com.contract_management.api.model.Ativo;
import com.contract_management.api.model.Secretaria;
import com.contract_management.api.repository.AtivoRepository;
import com.contract_management.api.repository.SecretariaRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class SecretariaService {

    private final SecretariaRepository secretariaRepository;
    private final AtivoRepository ativoRepository;

    @Transactional(readOnly = true)
    public List<SecretariaResponseDTO> listarTodos() {
        log.info("Buscando todas as secretarias");
        return secretariaRepository.findAll().stream()
                .map(this::toResponseDTO)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public SecretariaResponseDTO buscarPorId(Long id) {
        log.info("Buscando secretaria com ID: {}", id);
        Secretaria secretaria = secretariaRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Secretaria", id));
        return toResponseDTO(secretaria);
    }

    @Transactional
    public SecretariaResponseDTO criar(SecretariaRequestDTO dto) {
        log.info("Criando nova secretaria: {}", dto.getNome());

        if (secretariaRepository.findBySigla(dto.getSigla()).isPresent()) {
            throw new BusinessException("Sigla '" + dto.getSigla() + "' já está em uso");
        }

        Ativo ativo = ativoRepository.findById(dto.getAtivoId())
                .orElseThrow(() -> new EntityNotFoundException("Ativo", dto.getAtivoId()));

        Secretaria secretaria = Secretaria.builder()
                .nome(dto.getNome())
                .sigla(dto.getSigla())
                .ativo(ativo)
                .build();

        Secretaria saved = secretariaRepository.save(secretaria);
        log.info("Secretaria criada com sucesso. ID: {}", saved.getId());

        return toResponseDTO(saved);
    }

    @Transactional
    public SecretariaResponseDTO atualizar(Long id, SecretariaRequestDTO dto) {
        log.info("Atualizando secretaria ID: {}", id);

        Secretaria secretaria = secretariaRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Secretaria", id));

        if (!secretaria.getSigla().equals(dto.getSigla())) {
            if (secretariaRepository.findBySigla(dto.getSigla()).isPresent()) {
                throw new BusinessException("Sigla '" + dto.getSigla() + "' já está em uso");
            }
        }

        Ativo ativo = ativoRepository.findById(dto.getAtivoId())
                .orElseThrow(() -> new EntityNotFoundException("Ativo", dto.getAtivoId()));

        secretaria.setNome(dto.getNome());
        secretaria.setSigla(dto.getSigla());
        secretaria.setAtivo(ativo);

        Secretaria updated = secretariaRepository.save(secretaria);
        log.info("Secretaria atualizada com sucesso. ID: {}", updated.getId());

        return toResponseDTO(updated);
    }

    @Transactional
    public void deletar(Long id) {
        log.info("Deletando secretaria ID: {}", id);

        if (!secretariaRepository.existsById(id)) {
            throw new EntityNotFoundException("Secretaria", id);
        }

        secretariaRepository.deleteById(id);
        log.info("Secretaria deletada com sucesso. ID: {}", id);
    }

    private SecretariaResponseDTO toResponseDTO(Secretaria secretaria) {
        SecretariaResponseDTO dto = new SecretariaResponseDTO();
        dto.setId(secretaria.getId());
        dto.setNome(secretaria.getNome());
        dto.setSigla(secretaria.getSigla());
        dto.setSituacao(secretaria.getAtivo().getSituacao());
        return dto;
    }
}