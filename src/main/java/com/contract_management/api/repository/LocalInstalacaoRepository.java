package com.contract_management.api.repository;

import com.contract_management.api.model.LocalInstalacao;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface LocalInstalacaoRepository extends JpaRepository<LocalInstalacao, Long> {

    @Query("SELECT l FROM LocalInstalacao l " +
           "JOIN FETCH l.secretaria s " +
           "WHERE l.ativo = true " +
           "ORDER BY s.sigla ASC, l.nome ASC")
    List<LocalInstalacao> findAllAtivosComSecretaria();

    @Query("SELECT l FROM LocalInstalacao l " +
           "JOIN FETCH l.secretaria s " +
           "WHERE l.secretaria.id = :secretariaId AND l.ativo = true " +
           "ORDER BY l.nome ASC")
    List<LocalInstalacao> findBySecretariaIdAtivos(@Param("secretariaId") Long secretariaId);

    Optional<LocalInstalacao> findByNomeIgnoreCaseAndSecretariaId(String nome, Long secretariaId);
}
