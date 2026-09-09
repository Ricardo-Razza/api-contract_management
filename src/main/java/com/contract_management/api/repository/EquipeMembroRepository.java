package com.contract_management.api.repository;

import com.contract_management.api.model.EquipeMembro;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface EquipeMembroRepository extends JpaRepository<EquipeMembro, Long> {
}
