package com.contract_management.api.service;

import com.contract_management.api.dto.request.ServidorRequestDTO;
import com.contract_management.api.dto.response.ServidorResponseDTO;
import com.contract_management.api.exception.BusinessException;
import com.contract_management.api.exception.EntityNotFoundException;
import com.contract_management.api.model.Ativo;
import com.contract_management.api.model.Secretaria;
import com.contract_management.api.model.Servidor;
import com.contract_management.api.repository.AtivoRepository;
import com.contract_management.api.repository.SecretariaRepository;
import com.contract_management.api.repository.ServidorRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class ServidorService {

    private final ServidorRepository servidorRepository;
    private final SecretariaRepository secretariaRepository;
    private final AtivoRepository ativoRepository;

    @Transactional(readOnly = true)
    public List<ServidorResponseDTO> listarTodos() {
        log.info("Buscando todos os servidores");
        return servidorRepository.findAll().stream()
                .map(this::toResponseDTO)
                .collect(Collectors.toList());
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

        Servidor servidor = Servidor.builder()
                .nome(dto.getNome())
                .cargo(dto.getCargo())
                .matricula(dto.getMatricula())
                .email(dto.getEmail())
                .telefone(dto.getTelefone())
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

        servidor.setNome(dto.getNome());
        servidor.setCargo(dto.getCargo());
        servidor.setMatricula(dto.getMatricula());
        servidor.setEmail(dto.getEmail());
        servidor.setTelefone(dto.getTelefone());
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
        dto.setMatricula(servidor.getMatricula());
        dto.setEmail(servidor.getEmail());
        dto.setTelefone(servidor.getTelefone());
        dto.setSituacao(servidor.getAtivo().getSituacao());
        return dto;
    }
}