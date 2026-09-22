package com.contract_management.api.repository;

import com.contract_management.api.model.ColetaContadorItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ColetaContadorItemRepository extends JpaRepository<ColetaContadorItem, Long> {

    List<ColetaContadorItem> findBySessaoIdOrderByItemPedidoAsc(Long sessaoId);

    List<ColetaContadorItem> findBySessaoIdAndStatus(Long sessaoId, String status);
}
