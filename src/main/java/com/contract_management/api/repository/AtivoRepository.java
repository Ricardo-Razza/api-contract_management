package com.contract_management.api.repository;

import com.contract_management.api.model.Ativo;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface AtivoRepository extends JpaRepository<Ativo, Long> {
    Optional<Ativo> findBySituacao(String situacao);
}