package com.contract_management.api.modules.contrato.dto.request;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;
import java.time.LocalDate;
import java.util.List;

import com.contract_management.api.modules.contrato.model.Tipo;
import com.contract_management.api.modules.equipe.dto.request.MembroEquipeRequestDTO;
import jakarta.validation.Valid;

@Data
public class ContratoRequestDTO {

    @NotNull(message = "Número é obrigatório")
    @Min(value = 1, message = "Número deve ser maior que zero")
    private Integer numero;

    @NotNull(message = "Ano é obrigatório")
    @Min(value = 2000, message = "Ano deve ser maior que 2000")
    private Integer ano;

    @NotNull(message = "Data de início é obrigatória")
    private LocalDate dataInicio;

    @NotNull(message = "Data de fim é obrigatória")
    private LocalDate dataFim;

    @NotNull(message = "Tipo é obrigatório")
    private Long tipoId;

    @NotBlank(message = "Objeto é obrigatório")
    private String objeto;

    @NotBlank(message = "Nome do contratado é obrigatório")
    @Size(max = 255, message = "Nome do contratado deve ter no máximo 255 caracteres")
    private String nomeContratado;

    @NotBlank(message = "Portaria de designação é obrigatória")
    @Size(max = 100, message = "Portaria de designação deve ter no máximo 100 caracteres")
    private String portariaDesignacao;

    @NotNull(message = "Data de designação é obrigatória")
    private LocalDate dataDesignacao;

    @NotNull(message = "Status é obrigatório")
    private Long ativoId;

    private List<Long> secretariasIds;

    @Size(max = 255, message = "Observação deve ter no máximo 255 caracteres")
    private String observacao;

    private List<@Valid MembroEquipeRequestDTO> membros;

    @AssertTrue(message = "Data fim não pode ser anterior à data início")
    public boolean isPeriodoValido() {
        if (dataInicio == null || dataFim == null) {
            return true;
        }
        return !dataFim.isBefore(dataInicio);
    }
}