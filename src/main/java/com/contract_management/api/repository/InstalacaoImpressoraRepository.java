package com.contract_management.api.repository;

import com.contract_management.api.model.InstalacaoImpressora;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

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

    List<InstalacaoImpressora> findByImpressoraIdOrderByDataInstalacaoDesc(Long impressoraId);

    long countByEmpenhoIdAndStatus(Long empenhoId, String status);

    List<InstalacaoImpressora> findByEmpenhoIdAndStatus(Long empenhoId, String status);

    long countByLocalInstalacaoIgnoreCaseAndStatus(String localInstalacao, String status);
}
