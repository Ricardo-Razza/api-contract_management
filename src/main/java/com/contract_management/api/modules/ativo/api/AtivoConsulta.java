package com.contract_management.api.modules.ativo.api;

import com.contract_management.api.modules.ativo.model.Ativo;
import java.util.Optional;

/** Catálogo de situações disponível para os demais módulos. */
public interface AtivoConsulta {
    Optional<Ativo> findById(Long id);
}
