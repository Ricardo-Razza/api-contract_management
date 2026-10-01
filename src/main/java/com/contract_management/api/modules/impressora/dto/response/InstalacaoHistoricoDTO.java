package com.contract_management.api.modules.impressora.dto.response;

import java.time.LocalDate;
import com.contract_management.api.modules.impressora.model.InstalacaoImpressora;

public record InstalacaoHistoricoDTO(Long id, Long impressoraId, Integer itemPedido,
        String numeroSerie, String fabricante, String modelo, Long localInstalacaoId,
        String localInstalacao, Long secretariaId, String secretariaSigla,
        LocalDate dataInstalacao, LocalDate dataRetirada, String status, String motivoRetirada) {
    public static InstalacaoHistoricoDTO from(InstalacaoImpressora i) {
        var p = i.getImpressora();
        var s = i.getSecretaria();
        return new InstalacaoHistoricoDTO(i.getId(), p.getId(), p.getItemPedido(), p.getNumeroSerie(),
                p.getFabricante(), p.getModelo(), i.getLocalInstalacaoId(), i.getLocalInstalacao(),
                s != null ? s.getId() : null, s != null ? s.getSigla() : null,
                i.getDataInstalacao(), i.getDataRetirada(), i.getStatus(), i.getMotivoRetirada());
    }
}
