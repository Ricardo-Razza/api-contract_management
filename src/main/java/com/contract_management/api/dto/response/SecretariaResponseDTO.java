package com.contract_management.api.dto.response;

import lombok.Data;

@Data
public class SecretariaResponseDTO {

    private Long id;
    private String nome;
    private String sigla;
    private String situacao;
}