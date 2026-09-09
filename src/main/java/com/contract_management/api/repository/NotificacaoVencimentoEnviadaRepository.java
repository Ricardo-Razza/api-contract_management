package com.contract_management.api.repository;

import com.contract_management.api.model.NotificacaoVencimentoEnviada;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface NotificacaoVencimentoEnviadaRepository
        extends JpaRepository<NotificacaoVencimentoEnviada, Long> {

    @Query("""
           SELECT n.contrato.id FROM NotificacaoVencimentoEnviada n
           WHERE n.contrato.id IN :contratoIds AND n.diasAlerta = :diasAlerta
           """)
    List<Long> findContratoIdsJaNotificados(@Param("contratoIds") List<Long> contratoIds,
                                            @Param("diasAlerta") int diasAlerta);

    @Query("""
           SELECT n.ata.id FROM NotificacaoVencimentoEnviada n
           WHERE n.ata.id IN :ataIds AND n.diasAlerta = :diasAlerta
           """)
    List<Long> findAtaIdsJaNotificados(@Param("ataIds") List<Long> ataIds,
                                       @Param("diasAlerta") int diasAlerta);
}