package com.contract_management.api.modules.impressora.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

import com.contract_management.api.modules.impressora.model.ColetaContadorItem;

@Repository
public interface ColetaContadorItemRepository extends JpaRepository<ColetaContadorItem, Long> {

    List<ColetaContadorItem> findBySessaoIdOrderByItemPedidoAsc(Long sessaoId);

    List<ColetaContadorItem> findBySessaoIdAndStatus(Long sessaoId, String status);

    Optional<ColetaContadorItem> findFirstByImpressoraIdAndDataInicioColetaIsNotNullOrderByDataInicioColetaDesc(Long impressoraId);
}
