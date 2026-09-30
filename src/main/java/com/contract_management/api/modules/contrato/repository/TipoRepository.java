package com.contract_management.api.modules.contrato.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;

import com.contract_management.api.modules.contrato.model.Tipo;

@Repository
public interface TipoRepository extends JpaRepository<Tipo, Long> {
    Optional<Tipo> findByTipoArp(String tipoArp);
}