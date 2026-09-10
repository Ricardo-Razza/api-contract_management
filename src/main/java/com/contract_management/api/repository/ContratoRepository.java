package com.contract_management.api.repository;

import com.contract_management.api.model.Contrato;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface ContratoRepository extends JpaRepository<Contrato, Long> {

    @Override
    @EntityGraph(attributePaths = {"tipo", "ativo"})
    List<Contrato> findAll();

    @Override
    @EntityGraph(attributePaths = {"tipo", "ativo"})
    Optional<Contrato> findById(Long id);

    Optional<Contrato> findByNumeroAndAno(Integer numero, Integer ano);

    boolean existsByNumeroAndAno(Integer numero, Integer ano);

    List<Contrato> findByAtivoId(Long ativoId);

    List<Contrato> findByDataFimBefore(LocalDate data);


    @Query("""
        SELECT c FROM Contrato c
        WHERE c.dataFim = :data
          AND c.ativo.situacao = 'ATIVO'
        """)
    List<Contrato> findByDataFim(@Param("data") LocalDate data);

    // Alternativa via Derived Query Method (sem precisar da anotação @Query)
    List<Contrato> findByDataFimAndAtivoSituacao(LocalDate dataFim, String situacao);
}