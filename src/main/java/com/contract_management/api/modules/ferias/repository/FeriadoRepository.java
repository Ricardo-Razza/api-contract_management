package com.contract_management.api.modules.ferias.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import com.contract_management.api.modules.ferias.model.Feriado;

@Repository
public interface FeriadoRepository extends JpaRepository<Feriado, Long> {
    List<Feriado> findByDataBetweenOrderByDataAsc(LocalDate inicio, LocalDate fim);
    Optional<Feriado> findByData(LocalDate data);
    boolean existsByData(LocalDate data);
}