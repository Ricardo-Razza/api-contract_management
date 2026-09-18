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
public class SubstituicaoImpressoraRequestDTO {

    @NotNull(message = "O contador final mono da máquina retirada é obrigatório")
    private Integer contadorFinalMonoRetirada;

    private Integer contadorFinalColorRetirada;

    @NotNull(message = "A data da substituição é obrigatória")
    private LocalDate dataSubstituicao;

    @NotBlank(message = "O motivo da substituição é obrigatório")
    private String motivoDefeito;

    // Dados da nova impressora instalada
    private String novoNumeroSerie;

    @NotBlank(message = "O modelo da nova impressora é obrigatório")
    private String novoModelo;

    private String novoFabricante;

    @NotNull(message = "O contador inicial mono da nova máquina é obrigatório")
    private Integer contadorInicialMonoNova;

    private Integer contadorInicialColorNova;
}
