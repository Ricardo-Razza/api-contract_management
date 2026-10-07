package com.contract_management.api.modules.contrato.dto.request;

import lombok.Data;
import java.util.ArrayList;
import java.util.List;

/** Parâmetros opcionais das listagens; as rotas anteriores continuam disponíveis. */
@Data
public class DocumentoFiltro {
    private String search = "";
    private Integer ano;
    private String tipo = "";
    private String status = "";
    private String vigencia = "";
    private List<Long> secretarias = new ArrayList<>();
    private List<String> pessoas = new ArrayList<>();

    public boolean vazio() {
        return search.isBlank() && ano == null && tipo.isBlank() && status.isBlank()
                && vigencia.isBlank() && secretarias.isEmpty() && pessoas.isEmpty();
    }
}
