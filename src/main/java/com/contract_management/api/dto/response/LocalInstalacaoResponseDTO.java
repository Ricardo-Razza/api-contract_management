package com.contract_management.api.dto.response;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LocalInstalacaoResponseDTO {

    private Long id;
    private String nome;
    private Long secretariaId;
    private String secretariaNome;
    private String secretariaSigla;
    private String endereco;
    private String responsavel;
    private String telefone;
    private Boolean ativo;
    private Long quantidadeImpressorasAtivas;
}
