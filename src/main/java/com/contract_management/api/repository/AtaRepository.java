package com.contract_management.api.repository;

import com.contract_management.api.model.AtaRegistroPreco;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface AtaRepository extends JpaRepository<AtaRegistroPreco, Long> {

    Optional<AtaRegistroPreco> findByNumeroAndAno(Integer numero, Integer ano);
    List<AtaRegistroPreco> findByAtivoId(Long ativoId);
    List<AtaRegistroPreco> findByDataFimBefore(LocalDate data);
    List<AtaRegistroPreco> findByDataFim(LocalDate dataFim);


    /**
     * Carrega todas as ATAs junto com as secretarias vinculadas e seus dados (JOIN FETCH completo).
     * Inclui: AtaSecretaria → Secretaria e Ativo — elimina todo lazy loading.
     */
    @Query("SELECT DISTINCT a FROM AtaRegistroPreco a " +
           "LEFT JOIN FETCH a.secretarias s " +
           "LEFT JOIN FETCH s.secretaria " +
           "LEFT JOIN FETCH s.ativo")
    List<AtaRegistroPreco> findAllComSecretarias();

    /**
     * Carrega uma ATA por ID com todas as secretarias e dados relacionados (JOIN FETCH completo).
     */
    @Query("SELECT a FROM AtaRegistroPreco a " +
           "LEFT JOIN FETCH a.secretarias s " +
           "LEFT JOIN FETCH s.secretaria " +
           "LEFT JOIN FETCH s.ativo " +
           "WHERE a.id = :id")
    Optional<AtaRegistroPreco> findComSecretariasById(@Param("id") Long id);
}