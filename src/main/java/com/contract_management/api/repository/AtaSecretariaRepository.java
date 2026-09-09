package com.contract_management.api.repository;

import com.contract_management.api.model.AtaSecretaria;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AtaSecretariaRepository extends JpaRepository<AtaSecretaria, Long> {
    List<AtaSecretaria> findByAtaId(Long ataId);
    List<AtaSecretaria> findBySecretariaId(Long secretariaId);
    boolean existsByAtaIdAndSecretariaId(Long ataId, Long secretariaId);
    void deleteByAtaId(Long ataId);
}
