package com.contract_management.api.modules.contrato.repository;

import com.contract_management.api.modules.contrato.api.AtaConsulta;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import com.contract_management.api.modules.ativo.model.Ativo;
import com.contract_management.api.modules.contrato.model.AtaRegistroPreco;
import com.contract_management.api.modules.contrato.model.AtaSecretaria;
import com.contract_management.api.modules.equipe.model.EquipeContrato;
import com.contract_management.api.modules.secretaria.model.Secretaria;

@Repository
public interface AtaRepository extends JpaRepository<AtaRegistroPreco, Long>, AtaConsulta {
    @Query("SELECT DISTINCT a.ano FROM AtaRegistroPreco a ORDER BY a.ano DESC")
    List<Integer> listarAnos();

    @Query("SELECT DISTINCT a.tipo.tipoArp FROM AtaRegistroPreco a ORDER BY a.tipo.tipoArp")
    List<String> listarTipos();

    @Override
    Optional<AtaRegistroPreco> findById(Long id);


    Optional<AtaRegistroPreco> findByNumeroAndAno(Integer numero, Integer ano);
    List<AtaRegistroPreco> findByAtivoId(Long ativoId);
    List<AtaRegistroPreco> findByDataFimBefore(LocalDate data);
    List<AtaRegistroPreco> findByDataFim(LocalDate dataFim);


    /**
     * Carrega todas as ATAs junto com as secretarias vinculadas e seus dados (JOIN FETCH completo).
     * Inclui: tipo, ativo, AtaSecretaria → Secretaria e Ativo — elimina todo lazy loading.
     */
    @Query("SELECT DISTINCT a FROM AtaRegistroPreco a " +
           "LEFT JOIN FETCH a.tipo " +
           "LEFT JOIN FETCH a.ativo " +
           "LEFT JOIN FETCH a.secretarias s " +
           "LEFT JOIN FETCH s.secretaria " +
           "LEFT JOIN FETCH s.ativo")
    List<AtaRegistroPreco> findAllComSecretarias();

    /**
     * Carrega tipo, ativo e secretarias vinculadas para a lista de ATAs fornecida (utilizado na paginação).
     */
    @Query("SELECT DISTINCT a FROM AtaRegistroPreco a " +
           "LEFT JOIN FETCH a.tipo " +
           "LEFT JOIN FETCH a.ativo " +
           "LEFT JOIN FETCH a.secretarias s " +
           "LEFT JOIN FETCH s.secretaria " +
           "LEFT JOIN FETCH s.ativo " +
           "WHERE a IN :atas")
    List<AtaRegistroPreco> carregarSecretarias(@Param("atas") List<AtaRegistroPreco> atas);

    /**
     * Carrega as equipes para a lista de ATAs fornecida.
     */
    @Query("SELECT DISTINCT a FROM AtaRegistroPreco a " +
           "LEFT JOIN FETCH a.equipe eq " +
           "LEFT JOIN FETCH eq.ativo " +
           "WHERE a IN :atas")
    List<AtaRegistroPreco> carregarEquipes(@Param("atas") List<AtaRegistroPreco> atas);

    /**
     * Carrega os membros, servidores e funções das equipes das ATAs fornecidas.
     */
    @Query("SELECT DISTINCT eq FROM EquipeContrato eq " +
           "LEFT JOIN FETCH eq.membros m " +
           "LEFT JOIN FETCH m.servidor " +
           "LEFT JOIN FETCH m.funcao " +
           "WHERE eq.ata IN :atas")
    List<EquipeContrato> carregarMembrosEquipes(@Param("atas") List<AtaRegistroPreco> atas);

    /**
     * Carrega uma ATA por ID com todas as secretarias e dados relacionados (JOIN FETCH completo).
     */
    @Query("SELECT a FROM AtaRegistroPreco a " +
           "LEFT JOIN FETCH a.tipo " +
           "LEFT JOIN FETCH a.ativo " +
           "LEFT JOIN FETCH a.secretarias s " +
           "LEFT JOIN FETCH s.secretaria " +
           "LEFT JOIN FETCH s.ativo " +
           "WHERE a.id = :id")
    Optional<AtaRegistroPreco> findComSecretariasById(@Param("id") Long id);

    /**
     * Carrega as equipes para uma ATA específica por ID.
     */
    @Query("SELECT DISTINCT a FROM AtaRegistroPreco a " +
           "LEFT JOIN FETCH a.equipe eq " +
           "LEFT JOIN FETCH eq.ativo " +
           "WHERE a.id = :id")
    Optional<AtaRegistroPreco> findComEquipeById(@Param("id") Long id);

    /**
     * Carrega os membros, servidores e funções das equipes de uma ATA específica por ID.
     */
    @Query("SELECT DISTINCT eq FROM EquipeContrato eq " +
           "LEFT JOIN FETCH eq.membros m " +
           "LEFT JOIN FETCH m.servidor " +
           "LEFT JOIN FETCH m.funcao " +
           "WHERE eq.ata.id = :id")
    List<EquipeContrato> carregarMembrosEquipePorAtaId(@Param("id") Long id);
}
