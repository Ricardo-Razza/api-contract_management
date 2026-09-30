package com.contract_management.api.modules.secretaria.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

import com.contract_management.api.modules.secretaria.model.Secretaria;

@Repository
public interface SecretariaRepository extends JpaRepository<Secretaria, Long> {
    Optional<Secretaria> findBySigla(String sigla);
    List<Secretaria> findByAtivoId(Long ativoId);
}