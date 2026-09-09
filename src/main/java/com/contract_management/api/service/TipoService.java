package com.contract_management.api.service;

import com.contract_management.api.exception.EntityNotFoundException;
import com.contract_management.api.model.Tipo;
import com.contract_management.api.repository.TipoRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class TipoService {

    private final TipoRepository tipoRepository;

    public List<Tipo> listarTodos() {
        log.info("Buscando todos os tipos");
        return tipoRepository.findAll();
    }

    public Tipo buscarPorId(Long id) {
        log.info("Buscando tipo com ID: {}", id);
        return tipoRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Tipo", id));
    }

    public Tipo buscarPorTipoArp(String tipoArp) {
        log.info("Buscando tipo por nome: {}", tipoArp);
        return tipoRepository.findByTipoArp(tipoArp)
                .orElseThrow(() -> new EntityNotFoundException("Tipo", "tipoArp", tipoArp));
    }
}