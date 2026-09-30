package com.contract_management.api.modules.ferias.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.ArrayList;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VerificacaoConflitoResponseDTO {
    private boolean temConflito;
    private boolean bloqueante;
    private String mensagem;
    @Builder.Default
    private List<ConflitoItemDTO> conflitos = new ArrayList<>();
}