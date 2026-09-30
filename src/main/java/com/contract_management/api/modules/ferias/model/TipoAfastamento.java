package com.contract_management.api.modules.ferias.model;

import lombok.Getter;

@Getter
public enum TipoAfastamento {
    FERIAS("Férias", "#eab308"),
    LICENCA_SAUDE("Licença Saúde", "#ef4444"),
    LICENCA_PREMIO("Licença Prêmio", "#b45309"),
    FOLGA("Folga", "#4338ca");

    private final String descricao;
    private final String corPadrao;

    TipoAfastamento(String descricao, String corPadrao) {
        this.descricao = descricao;
        this.corPadrao = corPadrao;
    }
}