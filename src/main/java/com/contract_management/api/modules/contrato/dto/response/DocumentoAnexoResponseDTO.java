package com.contract_management.api.modules.contrato.dto.response;

import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DocumentoAnexoResponseDTO {

    private Long id;
    private String nomeOriginal;
    private String tipoDocumento;
    private String contentType;
    private Long tamanhoBytes;
    private String descricao;
    private Long contratoId;
    private Long ataId;
    private LocalDateTime criadoEm;
    private String urlDownload;
    private String urlVisualizar;
}
