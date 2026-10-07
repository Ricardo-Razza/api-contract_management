package com.contract_management.api.modules.servidor.api;

import com.contract_management.api.modules.servidor.model.Servidor;
import java.util.Optional;

/** Consultas de servidores, incluindo o bloqueio usado nas transações de férias. */
public interface ServidorConsulta {
    Optional<Servidor> findById(Long id);
    Optional<Servidor> bloquearPorId(Long id);
}
