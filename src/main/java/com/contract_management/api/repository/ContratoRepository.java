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

    /**
     * Carrega todos os contratos com tipo, ativo e secretarias vinculadas em uma única query com JOIN FETCH.
     */
    @Query("SELECT DISTINCT c FROM Contrato c " +
           "LEFT JOIN FETCH c.tipo " +
           "LEFT JOIN FETCH c.ativo " +
           "LEFT JOIN FETCH c.secretarias s " +
           "LEFT JOIN FETCH s.secretaria " +
           "LEFT JOIN FETCH s.ativo")
    List<Contrato> findAllComSecretarias();

    /**
     * Carrega tipo, ativo e secretarias vinculadas para a lista de contratos fornecida (utilizado na paginação).
     */
    @Query("SELECT DISTINCT c FROM Contrato c " +
           "LEFT JOIN FETCH c.tipo " +
           "LEFT JOIN FETCH c.ativo " +
           "LEFT JOIN FETCH c.secretarias s " +
           "LEFT JOIN FETCH s.secretaria " +
           "LEFT JOIN FETCH s.ativo " +
           "WHERE c IN :contratos")
    List<Contrato> carregarSecretarias(@Param("contratos") List<Contrato> contratos);

    /**
     * Carrega as equipes com membros, servidores e funções para a lista de contratos fornecida,
     * inicializando os relacionamentos no Persistence Context em uma única query sem MultipleBagFetchException.
     */
    @Query("SELECT DISTINCT c FROM Contrato c " +
           "LEFT JOIN FETCH c.equipe eq " +
           "LEFT JOIN FETCH eq.ativo " +
           "LEFT JOIN FETCH eq.membros m " +
           "LEFT JOIN FETCH m.servidor " +
           "LEFT JOIN FETCH m.funcao " +
           "WHERE c IN :contratos")
    List<Contrato> carregarEquipes(@Param("contratos") List<Contrato> contratos);

    /**
     * Carrega um contrato por ID com tipo, ativo e secretarias vinculadas.
     */
    @Query("SELECT c FROM Contrato c " +
           "LEFT JOIN FETCH c.tipo " +
           "LEFT JOIN FETCH c.ativo " +
           "LEFT JOIN FETCH c.secretarias s " +
           "LEFT JOIN FETCH s.secretaria " +
           "LEFT JOIN FETCH s.ativo " +
           "WHERE c.id = :id")
    Optional<Contrato> findComSecretariasById(@Param("id") Long id);

    /**
     * Carrega as equipes com membros, servidores e funções para um contrato específico por ID.
     */
    @Query("SELECT DISTINCT c FROM Contrato c " +
           "LEFT JOIN FETCH c.equipe eq " +
           "LEFT JOIN FETCH eq.ativo " +
           "LEFT JOIN FETCH eq.membros m " +
           "LEFT JOIN FETCH m.servidor " +
           "LEFT JOIN FETCH m.funcao " +
           "WHERE c.id = :id")
    Optional<Contrato> findComEquipeById(@Param("id") Long id);
}