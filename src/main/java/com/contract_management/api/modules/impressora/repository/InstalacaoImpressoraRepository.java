package com.contract_management.api.modules.impressora.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

import com.contract_management.api.modules.impressora.model.InstalacaoImpressora;

@Repository
public interface InstalacaoImpressoraRepository extends JpaRepository<InstalacaoImpressora, Long> {

    @Query("SELECT inst FROM InstalacaoImpressora inst " +
           "JOIN FETCH inst.secretaria " +
           "LEFT JOIN FETCH inst.empenho " +
           "WHERE inst.impressora.id = :impressoraId AND inst.status = 'ATIVA'")
    Optional<InstalacaoImpressora> findAtivaByImpressoraId(@Param("impressoraId") Long impressoraId);

    @Query("SELECT inst FROM InstalacaoImpressora inst " +
           "JOIN FETCH inst.impressora imp " +
           "LEFT JOIN FETCH imp.lote " +
           "JOIN FETCH inst.secretaria " +
           "LEFT JOIN FETCH inst.empenho " +
           "WHERE inst.status = 'ATIVA' ORDER BY imp.itemPedido ASC")
    List<InstalacaoImpressora> findAllAtivasWithDetails();

    @Query("SELECT i FROM InstalacaoImpressora i JOIN FETCH i.impressora JOIN FETCH i.secretaria " +
           "WHERE i.impressora.id = :impressoraId ORDER BY i.dataInstalacao DESC, i.id DESC")
    List<InstalacaoImpressora> findByImpressoraIdOrderByDataInstalacaoDesc(@Param("impressoraId") Long impressoraId);

    @Query("SELECT i FROM InstalacaoImpressora i JOIN FETCH i.impressora JOIN FETCH i.secretaria " +
           "WHERE i.localInstalacaoId = :localId ORDER BY i.dataInstalacao DESC, i.id DESC")
    List<InstalacaoImpressora> findHistoricoByLocalId(@Param("localId") Long localId);

    long countByLocalInstalacaoIdAndStatus(Long localInstalacaoId, String status);

    long countByEmpenhoIdAndStatus(Long empenhoId, String status);

    List<InstalacaoImpressora> findByEmpenhoIdAndStatus(Long empenhoId, String status);

    long countByLocalInstalacaoIgnoreCaseAndStatus(String localInstalacao, String status);
}