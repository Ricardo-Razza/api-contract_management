package com.contract_management.api.modules.servidor.dto.response;

import lombok.Data;

@Data
public class ServidorResponseDTO {

    private Long id;
    private String nome;
    private String cargo;
    private String setor;
    private Integer matricula;
    private String email;
    private String telefone;
    private Long secretariaId;
    private String secretariaNome;
    private String secretariaSigla;
    private String situacao;
}