package com.contract_management.api.repository;

import com.contract_management.api.model.FuncaoEquipe;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface FuncaoEquipeRepository extends JpaRepository<FuncaoEquipe, Long> {
    Optional<FuncaoEquipe> findByNome(String nome);
}