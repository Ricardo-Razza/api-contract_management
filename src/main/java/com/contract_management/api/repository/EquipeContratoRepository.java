package com.contract_management.api.repository;

import com.contract_management.api.model.EquipeContrato;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface EquipeContratoRepository extends JpaRepository<EquipeContrato, Long> {
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
