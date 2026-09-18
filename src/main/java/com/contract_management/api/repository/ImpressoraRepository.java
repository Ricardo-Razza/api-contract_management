package com.contract_management.api.repository;

import com.contract_management.api.model.Impressora;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ImpressoraRepository extends JpaRepository<Impressora, Long> {

    @Query("SELECT DISTINCT i FROM Impressora i " +
           "LEFT JOIN FETCH i.lote " +
           "WHERE i.ativo = true ORDER BY i.itemPedido ASC")
    List<Impressora> findAllActiveWithLote();

    Optional<Impressora> findByIp(String ip);

    Optional<Impressora> findByNumeroSerie(String numeroSerie);

    Optional<Impressora> findByItemPedido(Integer itemPedido);

    long countByLoteIdAndAtivoTrue(Long loteId);
}
