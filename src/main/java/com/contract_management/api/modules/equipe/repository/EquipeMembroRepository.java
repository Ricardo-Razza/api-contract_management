package com.contract_management.api.modules.equipe.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.contract_management.api.modules.equipe.model.EquipeMembro;

@Repository
public interface EquipeMembroRepository extends JpaRepository<EquipeMembro, Long> {
}