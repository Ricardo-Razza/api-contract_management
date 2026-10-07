package com.contract_management.api.modules.servidor.repository;

import com.contract_management.api.modules.servidor.api.ServidorConsulta;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

import com.contract_management.api.modules.servidor.model.Servidor;

@Repository
public interface ServidorRepository extends JpaRepository<Servidor, Long>, ServidorConsulta {
    @Override
    Optional<Servidor> findById(Long id);

    @org.springframework.data.jpa.repository.Lock(jakarta.persistence.LockModeType.PESSIMISTIC_WRITE)
    @org.springframework.data.jpa.repository.Query("SELECT s FROM Servidor s WHERE s.id = :id")
    Optional<Servidor> bloquearPorId(@org.springframework.data.repository.query.Param("id") Long id);

    List<Servidor> findByAtivoId(Long ativoId);
    List<Servidor> findAllByNomeIgnoreCase(String nome);
}
