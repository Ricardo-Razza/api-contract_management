package com.contract_management.api.modules.ativo.repository;

import com.contract_management.api.modules.ativo.api.AtivoConsulta;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;

import com.contract_management.api.modules.ativo.model.Ativo;

@Repository
public interface AtivoRepository extends JpaRepository<Ativo, Long>, AtivoConsulta {
    @Override
    Optional<Ativo> findById(Long id);

    Optional<Ativo> findBySituacao(String situacao);
}
