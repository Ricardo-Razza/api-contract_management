package com.contract_management.api.modules.equipe.repository;

import com.contract_management.api.modules.equipe.api.EquipesVinculadas;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.List;

import com.contract_management.api.modules.equipe.model.EquipeContrato;

@Repository
public interface EquipeContratoRepository extends JpaRepository<EquipeContrato, Long>, EquipesVinculadas {
    @Override
    <S extends EquipeContrato> S save(S equipe);

    @Override
    void deleteAll(Iterable<? extends EquipeContrato> equipes);

    @Override
    void flush();

    List<EquipeContrato> findByContratoId(Long contratoId);
    List<EquipeContrato> findByAtaId(Long ataId);

    @Query("""
        SELECT DISTINCT eq FROM EquipeContrato eq
        JOIN FETCH eq.ativo
        LEFT JOIN FETCH eq.membros m
        LEFT JOIN FETCH m.servidor
        LEFT JOIN FETCH m.funcao
        WHERE eq.contrato.id IN :contratoIds
        """)
    List<EquipeContrato> findByContratoIdInComMembros(@Param("contratoIds") List<Long> contratoIds);

    @Query("""
           SELECT DISTINCT ea FROM EquipeContrato ea
           JOIN FETCH ea.membros m
           JOIN FETCH m.servidor
           WHERE ea.ata.id IN :ataIds
           """)
    List<EquipeContrato> findByAtaIdInComMembros(@Param("ataIds") List<Long> ataIds);
}
