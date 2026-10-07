package com.contract_management.api.modules.contrato.api;

import com.contract_management.api.modules.contrato.model.Contrato;
import java.util.Optional;

/** Consulta de contratos disponível para composição das equipes. */
public interface ContratoConsulta {
    Optional<Contrato> findById(Long id);
}
