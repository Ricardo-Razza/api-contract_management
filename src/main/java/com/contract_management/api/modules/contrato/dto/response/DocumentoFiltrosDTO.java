package com.contract_management.api.modules.contrato.dto.response;

import java.util.List;

public record DocumentoFiltrosDTO(List<Integer> anos, List<String> tipos) {}
