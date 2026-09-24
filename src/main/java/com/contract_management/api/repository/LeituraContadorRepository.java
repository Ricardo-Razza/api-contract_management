package com.contract_management.api.repository;

import com.contract_management.api.model.LeituraContador;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface LeituraContadorRepository extends JpaRepository<LeituraContador, Long> {

    @Query("SELECT l FROM LeituraContador l " +
           "JOIN FETCH l.impressora imp " +
           "LEFT JOIN FETCH imp.lote " +
           "LEFT JOIN FETCH l.instalacao inst " +
           "LEFT JOIN FETCH inst.secretaria " +
           "LEFT JOIN FETCH inst.empenho " +
           "WHERE l.mesReferencia = :mes AND l.anoReferencia = :ano " +
           "ORDER BY imp.itemPedido ASC, l.id ASC")
    List<LeituraContador> findByMesAndAnoWithDetails(@Param("mes") Integer mes, @Param("ano") Integer ano);

    @Query("SELECT l FROM LeituraContador l " +
           "JOIN FETCH l.impressora imp " +
           "LEFT JOIN FETCH imp.lote " +
           "LEFT JOIN FETCH l.instalacao inst " +
           "LEFT JOIN FETCH inst.secretaria " +
           "LEFT JOIN FETCH inst.empenho " +
           "WHERE l.anoReferencia = :ano " +
           "ORDER BY l.mesReferencia ASC, imp.itemPedido ASC, l.id ASC")
    List<LeituraContador> findByAnoWithDetails(@Param("ano") Integer ano);

    @Query("SELECT l FROM LeituraContador l " +
           "JOIN FETCH l.impressora imp " +
           "LEFT JOIN FETCH imp.lote " +
           "LEFT JOIN FETCH l.instalacao inst " +
           "LEFT JOIN FETCH inst.secretaria " +
           "LEFT JOIN FETCH inst.empenho emp " +
           "WHERE l.mesReferencia = :mes AND l.anoReferencia = :ano AND emp.id = :empenhoId " +
           "ORDER BY imp.itemPedido ASC, l.id ASC")
    List<LeituraContador> findByMesAndAnoAndEmpenhoIdWithDetails(
            @Param("mes") Integer mes,
            @Param("ano") Integer ano,
            @Param("empenhoId") Long empenhoId);

    Optional<LeituraContador> findByImpressoraIdAndMesReferenciaAndAnoReferencia(
            Long impressoraId, Integer mesReferencia, Integer anoReferencia);

    @Query("SELECT l FROM LeituraContador l " +
           "WHERE l.impressora.id = :impressoraId " +
           "ORDER BY l.anoReferencia DESC, l.mesReferencia DESC")
    List<LeituraContador> findUltimasLeiturasPorImpressora(@Param("impressoraId") Long impressoraId);

    @Query("SELECT l FROM LeituraContador l " +
           "JOIN l.impressora imp " +
           "WHERE imp.itemPedido = :itemPedido " +
           "AND l.mesReferencia = :mes " +
           "AND l.anoReferencia = :ano " +
           "AND imp.id <> :impressoraId " +
           "AND l.origemLeitura = 'SWAP_RETIRADA'")
    List<LeituraContador> findSwapsRetiradaPorItemPedidoEMes(
            @Param("itemPedido") Integer itemPedido,
            @Param("mes") Integer mes,
            @Param("ano") Integer ano,
            @Param("impressoraId") Long impressoraId);
}
