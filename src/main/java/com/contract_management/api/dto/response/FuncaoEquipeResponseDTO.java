package com.contract_management.api.dto.response;

import lombok.Data;

@Data
public class FuncaoEquipeResponseDTO {
    private Long id;
    private String nome;
    private String descricao;
    private String situacao;
}