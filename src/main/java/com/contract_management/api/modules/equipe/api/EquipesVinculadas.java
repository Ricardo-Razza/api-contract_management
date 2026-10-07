package com.contract_management.api.modules.equipe.api;

import com.contract_management.api.modules.equipe.model.EquipeContrato;
import java.util.List;

/** Operações de equipes que participam da transação de edição de contratos e atas. */
public interface EquipesVinculadas {
    List<EquipeContrato> findByContratoId(Long contratoId);
    List<EquipeContrato> findByAtaId(Long ataId);
    List<EquipeContrato> findByContratoIdInComMembros(List<Long> contratoIds);
    List<EquipeContrato> findByAtaIdInComMembros(List<Long> ataIds);
    <S extends EquipeContrato> S save(S equipe);
    void deleteAll(Iterable<? extends EquipeContrato> equipes);
    void flush();
}
