package com.contract_management.api.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LocalInstalacaoRequestDTO {

    @NotBlank(message = "O nome do local é obrigatório")
    private String nome;

    @NotNull(message = "A secretaria é obrigatória")
    private Long secretariaId;

    private String endereco;
    private String responsavel;
    private String telefone;
    private Boolean ativo;
}
