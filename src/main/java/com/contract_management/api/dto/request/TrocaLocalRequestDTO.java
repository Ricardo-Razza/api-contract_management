package com.contract_management.api.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.time.LocalDate;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TrocaLocalRequestDTO {

    private Long localInstalacaoId;

    @NotNull(message = "A nova secretaria é obrigatória")
    private Long novaSecretariaId;

    private String novoLocalInstalacao;

    private String novoEndereco;
    private String novoResponsavel;
    private String novoIp;
    private String novoTransformador;

    @NotNull(message = "A data da mudança é obrigatória")
    private LocalDate dataMudanca;

    private Integer contadorAtualMono;
    private Integer contadorAtualColor;
    private String motivo;
}
