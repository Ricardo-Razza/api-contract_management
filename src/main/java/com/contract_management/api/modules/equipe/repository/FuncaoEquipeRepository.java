package com.contract_management.api.modules.equipe.repository;

import com.contract_management.api.modules.equipe.api.FuncaoEquipeConsulta;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;

import com.contract_management.api.modules.equipe.model.FuncaoEquipe;

@Repository
public interface FuncaoEquipeRepository extends JpaRepository<FuncaoEquipe, Long>, FuncaoEquipeConsulta {
    @Override
    Optional<FuncaoEquipe> findById(Long id);

    Optional<FuncaoEquipe> findByNome(String nome);
}
