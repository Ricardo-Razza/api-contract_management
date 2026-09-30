package com.contract_management.api.modules.contrato.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import java.util.List;

import com.contract_management.api.common.exception.EntityNotFoundException;
import com.contract_management.api.modules.contrato.model.Tipo;
import com.contract_management.api.modules.contrato.repository.TipoRepository;

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