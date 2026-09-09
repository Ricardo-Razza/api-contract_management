package com.contract_management.api.repository;

import com.contract_management.api.model.Servidor;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ServidorRepository extends JpaRepository<Servidor, Long> {
    List<Servidor> findByAtivoId(Long ativoId);
    List<Servidor> findAllByNomeIgnoreCase(String nome);
}
