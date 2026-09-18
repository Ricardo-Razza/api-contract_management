package com.contract_management.api.repository;

import com.contract_management.api.model.EmpenhoImpressao;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface EmpenhoImpressaoRepository extends JpaRepository<EmpenhoImpressao, Long> {
    List<EmpenhoImpressao> findByAtivoTrueOrderByNumeroEmpenhoAsc();
    List<EmpenhoImpressao> findBySecretariaIdAndAtivoTrue(Long secretariaId);
}
