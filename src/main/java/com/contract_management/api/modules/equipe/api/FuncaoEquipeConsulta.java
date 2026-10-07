package com.contract_management.api.modules.equipe.api;

import com.contract_management.api.modules.equipe.model.FuncaoEquipe;
import java.util.Optional;

/** Catálogo de funções utilizado ao compor equipes de contratos e atas. */
public interface FuncaoEquipeConsulta {
    Optional<FuncaoEquipe> findById(Long id);
}
