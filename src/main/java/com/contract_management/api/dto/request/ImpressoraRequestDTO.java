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
public class ImpressoraRequestDTO {

    private Integer itemPedido;
    private String numeroSerie;

    @NotBlank(message = "O fabricante é obrigatório")
    private String fabricante;

    @NotBlank(message = "O modelo é obrigatório")
    private String modelo;

    private String tipoImpressao;
    private Long loteId;
    private String ip;

    @NotNull(message = "A secretaria de instalação é obrigatória")
    private Long secretariaId;

    private Long empenhoId;

    @NotBlank(message = "O local de instalação é obrigatório")
    private String localInstalacao;

    private String endereco;
    private String responsavel;
    private String transformador;

    private LocalDate dataInstalacao;
    private Integer contadorInicialMono;
    private Integer contadorInicialColor;
}
