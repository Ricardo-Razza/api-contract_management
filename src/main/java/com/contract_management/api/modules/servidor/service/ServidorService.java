package com.contract_management.api.modules.servidor.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;
import java.util.stream.Collectors;

import com.contract_management.api.common.exception.EntityNotFoundException;
import com.contract_management.api.common.model.Ativo;
import com.contract_management.api.common.repository.AtivoRepository;
import com.contract_management.api.modules.secretaria.model.Secretaria;
import com.contract_management.api.modules.secretaria.repository.SecretariaRepository;
import com.contract_management.api.modules.servidor.dto.request.ServidorRequestDTO;
import com.contract_management.api.modules.servidor.dto.response.ServidorResponseDTO;
import com.contract_management.api.modules.servidor.model.Servidor;
import com.contract_management.api.modules.servidor.repository.ServidorRepository;

@Service
@RequiredArgsConstructor
@Slf4j
public class ServidorService {

    private final ServidorRepository servidorRepository;
    private final AtivoRepository ativoRepository;
    private final SecretariaRepository secretariaRepository;

    @Transactional(readOnly = true)
    public List<ServidorResponseDTO> listarTodos() {
        log.info("Buscando todos os servidores");
        return servidorRepository.findAll().stream()
                .map(this::toResponseDTO)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public Page<ServidorResponseDTO> listarPaginado(Pageable pageable) {
        log.info("Buscando servidores paginados: {}", pageable);
        return servidorRepository.findAll(pageable).map(this::toResponseDTO);
    }

    @Transactional(readOnly = true)
    public ServidorResponseDTO buscarPorId(Long id) {
        log.info("Buscando servidor com ID: {}", id);
        Servidor servidor = servidorRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Servidor", id));
        return toResponseDTO(servidor);
    }

    @Transactional
    public ServidorResponseDTO criar(ServidorRequestDTO dto) {
        log.info("Criando novo servidor: {}", dto.getNome());


        Ativo ativo = ativoRepository.findById(dto.getAtivoId())
                .orElseThrow(() -> new EntityNotFoundException("Ativo", dto.getAtivoId()));

        Secretaria secretaria = null;
        if (dto.getSecretariaId() != null) {
            secretaria = secretariaRepository.findById(dto.getSecretariaId())
                    .orElseThrow(() -> new EntityNotFoundException("Secretaria", dto.getSecretariaId()));
        }

        Servidor servidor = Servidor.builder()
                .nome(dto.getNome())
                .cargo(dto.getCargo())
                .setor(dto.getSetor())
                .matricula(dto.getMatricula())
                .email(dto.getEmail())
                .telefone(dto.getTelefone())
                .secretaria(secretaria)
                .ativo(ativo)
                .build();

        Servidor saved = servidorRepository.save(servidor);
        log.info("Servidor criado com sucesso. ID: {}", saved.getId());

        return toResponseDTO(saved);
    }

    @Transactional
    public ServidorResponseDTO atualizar(Long id, ServidorRequestDTO dto) {
        log.info("Atualizando servidor ID: {}", id);

        Servidor servidor = servidorRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Servidor", id));

        Ativo ativo = ativoRepository.findById(dto.getAtivoId())
                .orElseThrow(() -> new EntityNotFoundException("Ativo", dto.getAtivoId()));

        Secretaria secretaria = null;
        if (dto.getSecretariaId() != null) {
            secretaria = secretariaRepository.findById(dto.getSecretariaId())
                    .orElseThrow(() -> new EntityNotFoundException("Secretaria", dto.getSecretariaId()));
        }

        servidor.setNome(dto.getNome());
        servidor.setCargo(dto.getCargo());
        servidor.setSetor(dto.getSetor());
        servidor.setMatricula(dto.getMatricula());
        servidor.setEmail(dto.getEmail());
        servidor.setTelefone(dto.getTelefone());
        servidor.setSecretaria(secretaria);
        servidor.setAtivo(ativo);

        Servidor updated = servidorRepository.save(servidor);
        log.info("Servidor atualizado com sucesso. ID: {}", updated.getId());

        return toResponseDTO(updated);
    }

    @Transactional
    public void deletar(Long id) {
        log.info("Deletando servidor ID: {}", id);

        if (!servidorRepository.existsById(id)) {
            throw new EntityNotFoundException("Servidor", id);
        }

        servidorRepository.deleteById(id);
        log.info("Servidor deletado com sucesso. ID: {}", id);
    }

    private ServidorResponseDTO toResponseDTO(Servidor servidor) {
        ServidorResponseDTO dto = new ServidorResponseDTO();
        dto.setId(servidor.getId());
        dto.setNome(servidor.getNome());
        dto.setCargo(servidor.getCargo());
        dto.setSetor(servidor.getSetor());
        dto.setMatricula(servidor.getMatricula());
        dto.setEmail(servidor.getEmail());
        dto.setTelefone(servidor.getTelefone());
        if (servidor.getSecretaria() != null) {
            dto.setSecretariaId(servidor.getSecretaria().getId());
            dto.setSecretariaNome(servidor.getSecretaria().getNome());
            dto.setSecretariaSigla(servidor.getSecretaria().getSigla());
        }
        if (servidor.getAtivo() != null) {
            dto.setSituacao(servidor.getAtivo().getSituacao());
        }
        return dto;
    }
}