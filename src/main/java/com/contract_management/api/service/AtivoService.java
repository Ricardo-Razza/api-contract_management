package com.contract_management.api.service;

import com.contract_management.api.exception.EntityNotFoundException;
import com.contract_management.api.model.Ativo;
import com.contract_management.api.repository.AtivoRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class AtivoService {

    private final AtivoRepository ativoRepository;

    public List<Ativo> listarTodos() {
        log.info("Buscando todos os ativos");
        return ativoRepository.findAll();
    }

    public Ativo buscarPorId(Long id) {
        log.info("Buscando ativo com ID: {}", id);
        return ativoRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Ativo", id));
    }

    public Ativo buscarPorSituacao(String situacao) {
        log.info("Buscando ativo por situação: {}", situacao);
        return ativoRepository.findBySituacao(situacao)
                .orElseThrow(() -> new EntityNotFoundException("Ativo", "situacao", situacao));
    }
}