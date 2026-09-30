package com.contract_management.api.modules.impressora.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

import com.contract_management.api.modules.impressora.model.LoteImpressao;

@Repository
public interface LoteImpressaoRepository extends JpaRepository<LoteImpressao, Long> {
    List<LoteImpressao> findByAtivoTrueOrderByNumeroLoteAsc();
    Optional<LoteImpressao> findByNumeroLote(Integer numeroLote);
}