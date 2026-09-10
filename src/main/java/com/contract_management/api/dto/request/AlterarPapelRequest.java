package com.contract_management.api.dto.request;

import com.contract_management.api.model.Papel;
import jakarta.validation.constraints.NotNull;

public record AlterarPapelRequest(@NotNull Papel papel) {
}
