package com.contract_management.api.modules.contrato.repository;

import com.contract_management.api.modules.contrato.model.DocumentoAnexo;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface DocumentoAnexoRepository extends JpaRepository<DocumentoAnexo, Long> {

    List<DocumentoAnexo> findByContratoIdOrderByCriadoEmDesc(Long contratoId);

    List<DocumentoAnexo> findByAtaIdOrderByCriadoEmDesc(Long ataId);
}
