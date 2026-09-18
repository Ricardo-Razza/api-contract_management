package com.contract_management.api.repository;

import com.contract_management.api.model.LoteImpressao;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface LoteImpressaoRepository extends JpaRepository<LoteImpressao, Long> {
    List<LoteImpressao> findByAtivoTrueOrderByNumeroLoteAsc();
}
