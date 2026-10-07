package com.contract_management.api.modules.secretaria.api;

import com.contract_management.api.modules.secretaria.model.Secretaria;
import java.util.List;
import java.util.Optional;

/** Operações de secretarias disponíveis para os demais módulos. */
public interface SecretariaConsulta {
    Optional<Secretaria> findById(Long id);
    List<Secretaria> findAllById(Iterable<Long> ids);
}
