package com.contract_management.api.modules.contrato.api;

import com.contract_management.api.modules.contrato.model.AtaRegistroPreco;
import java.util.Optional;

/** Consulta de atas disponível para composição das equipes. */
public interface AtaConsulta {
    Optional<AtaRegistroPreco> findById(Long id);
}
