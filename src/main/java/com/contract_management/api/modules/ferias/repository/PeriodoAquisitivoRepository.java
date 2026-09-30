package com.contract_management.api.modules.ferias.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

import com.contract_management.api.modules.ferias.model.PeriodoAquisitivo;

@Repository
public interface PeriodoAquisitivoRepository extends JpaRepository<PeriodoAquisitivo, Long> {
    @org.springframework.data.jpa.repository.Lock(jakarta.persistence.LockModeType.PESSIMISTIC_WRITE)
    @org.springframework.data.jpa.repository.Query("SELECT p FROM PeriodoAquisitivo p WHERE p.id = :id")
    Optional<PeriodoAquisitivo> bloquearPorId(@org.springframework.data.repository.query.Param("id") Long id);
    @org.springframework.data.jpa.repository.Query("SELECT p FROM PeriodoAquisitivo p JOIN FETCH p.servidor s LEFT JOIN FETCH s.secretaria ORDER BY s.nome, p.dataInicio")
    List<PeriodoAquisitivo> listarTodos();
    List<PeriodoAquisitivo> findByServidorIdOrderByAnoInicioDesc(Long servidorId);
    List<PeriodoAquisitivo> findByServidorIdAndDiasRestantesGreaterThan(Long servidorId, Integer minDias);
    Optional<PeriodoAquisitivo> findByServidorIdAndAnoInicioAndAnoFim(Long servidorId, Integer anoInicio, Integer anoFim);
}