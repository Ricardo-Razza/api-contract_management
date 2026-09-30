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
public class EscalaAnualDTO {

    private Integer ano;
    private Long secretariaId;
    private String secretariaNome;
    private String secretariaSigla;
    private String setor;

    @Builder.Default
    private List<MesEscalaDTO> meses = new ArrayList<>();

    @Builder.Default
    private List<LegendaItemDTO> legendas = new ArrayList<>();

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class MesEscalaDTO {
        private Integer mesNumero; // 1 a 12
        private String mesNome;   // Ex: "Janeiro/2026"
        private Integer totalDias; // 28 a 31
        @Builder.Default
        private List<DiaInfoDTO> dias = new ArrayList<>();
        @Builder.Default
        private List<LinhaServidorMesDTO> linhas = new ArrayList<>();
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DiaInfoDTO {
        private Integer dia;
        private Integer diaSemana; // 1 = Seg, 7 = Dom
        private String diaSemanaSigla; // Seg, Ter, Qua, Qui, Sex, Sáb, Dom
        private boolean ehFimDeSemana;
        private boolean ehFeriado;
        private String nomeFeriado;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class LinhaServidorMesDTO {
        private Long servidorId;
        private String servidorNome;
        private Integer servidorMatricula;
        private String servidorCargo;
        private String servidorSetor;
        @Builder.Default
        private List<CelulaDiaDTO> celulas = new ArrayList<>(); // Índice 0 = Dia 1, ...
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CelulaDiaDTO {
        private Integer dia;
        private boolean ocupado;
        private Long agendamentoId;
        private String tipoAfastamento;
        private String tipoDescricao;
        private Long periodoAquisitivoId;
        private String periodoIdentificador;
        private String corHex;
        private Integer fracao;
        private String status;
        private boolean alertaConflito;
        private String descricaoConflito;
        private String observacao;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class LegendaItemDTO {
        private String label;
        private String corHex;
        private String tipo; // 'PERIODO', 'AFASTAMENTO', 'SISTEMA'
    }
}