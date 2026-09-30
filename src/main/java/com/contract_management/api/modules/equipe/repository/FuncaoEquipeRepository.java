package com.contract_management.api.modules.equipe.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;

import com.contract_management.api.modules.equipe.model.FuncaoEquipe;

@Repository
public interface FuncaoEquipeRepository extends JpaRepository<FuncaoEquipe, Long> {
    Optional<FuncaoEquipe> findByNome(String nome);
}