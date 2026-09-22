package com.contract_management.api.dto.response;

import lombok.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ColetaSessaoDTO {

    private Long id;
    private Integer anoReferencia;
    private Integer mesReferencia;
    private LocalDateTime dataInicio;
    private LocalDateTime dataFim;
    private String status;
    private Integer totalImpressoras;
    private Integer totalSucesso;
    private Integer totalFalhas;
    private String diretorioPrints;
    private String urlDownloadZip;

    @Builder.Default
    private List<ColetaItemDTO> itens = new ArrayList<>();
}
