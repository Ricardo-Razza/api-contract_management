package com.contract_management.api.modules.ferias.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.time.LocalDate;
import java.util.List;

import com.contract_management.api.modules.ferias.model.AgendamentoFerias;

@Repository
public interface AgendamentoFeriasRepository extends JpaRepository<AgendamentoFerias, Long> {

    List<AgendamentoFerias> findByServidorIdOrderByDataInicioAsc(Long servidorId);

    List<AgendamentoFerias> findByPeriodoAquisitivoId(Long periodoAquisitivoId);
    @Query("SELECT a FROM AgendamentoFerias a JOIN FETCH a.servidor s LEFT JOIN FETCH s.secretaria LEFT JOIN FETCH a.periodoAquisitivo WHERE a.dataInicio <= :fim AND a.dataFim >= :inicio ORDER BY a.dataInicio, s.nome")
    List<AgendamentoFerias> listarIntervalo(@Param("inicio") LocalDate inicio, @Param("fim") LocalDate fim);

    @Query("SELECT a FROM AgendamentoFerias a " +
           "JOIN FETCH a.servidor s " +
           "LEFT JOIN FETCH s.secretaria sec " +
           "LEFT JOIN FETCH a.periodoAquisitivo p " +
           "WHERE a.status <> 'CANCELADO' " +
           "AND a.dataInicio <= :fimAno " +
           "AND a.dataFim >= :inicioAno " +
           "AND (:secretariaId IS NULL OR sec.id = :secretariaId) " +
           "AND (:setor IS NULL OR :setor = '' OR LOWER(s.setor) LIKE LOWER(CONCAT('%', :setor, '%'))) " +
           "ORDER BY s.nome ASC, a.dataInicio ASC")
    List<AgendamentoFerias> findParaEscalaAnual(
            @Param("inicioAno") LocalDate inicioAno,
            @Param("fimAno") LocalDate fimAno,
            @Param("secretariaId") Long secretariaId,
            @Param("setor") String setor);

    @Query("SELECT a FROM AgendamentoFerias a " +
           "JOIN FETCH a.servidor s " +
           "LEFT JOIN FETCH s.secretaria sec " +
           "WHERE a.status <> 'CANCELADO' " +
           "AND (:agendamentoId IS NULL OR a.id <> :agendamentoId) " +

           "AND a.dataInicio <= :dataFim " +
           "AND a.dataFim >= :dataInicio " +
           "AND (" +
           "  s.id = :servidorId OR (sec.id = :secretariaId AND " +
           "  (:setor IS NOT NULL AND :setor <> '' AND LOWER(TRIM(s.setor)) = LOWER(TRIM(:setor))))" +
           ")")
    List<AgendamentoFerias> findConflitos(
            @Param("servidorId") Long servidorId,
            @Param("dataInicio") LocalDate dataInicio,
            @Param("dataFim") LocalDate dataFim,
            @Param("secretariaId") Long secretariaId,
            @Param("setor") String setor,
            @Param("agendamentoId") Long agendamentoId);
}