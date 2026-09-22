package com.contract_management.api.repository;

import com.contract_management.api.model.ColetaContadorSessao;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ColetaContadorSessaoRepository extends JpaRepository<ColetaContadorSessao, Long> {

    Optional<ColetaContadorSessao> findFirstByStatusOrderByDataInicioDesc(String status);

    @Query("SELECT s FROM ColetaContadorSessao s ORDER BY s.dataInicio DESC LIMIT 1")
    Optional<ColetaContadorSessao> findUltimaSessao();
}
