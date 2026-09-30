package com.contract_management.api.modules.ferias.dto.response;

import lombok.Data;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
public class PeriodoAquisitivoResponseDTO {

    private Long id;
    private Long servidorId;
    private String servidorNome;
    private Integer servidorMatricula;
    private Integer anoInicio;
    private Integer anoFim;
    private String identificador;
    private LocalDate dataInicio;
    private LocalDate dataFim;
    private LocalDate limiteGozo;
    private Integer totalDias;
    private Integer diasUsados;
    private Integer diasReservados;
    private Integer diasGozados;
    private Long secretariaId;
    private String servidorSetor;
    private Integer diasRestantes;
    private String corHex;
    private LocalDateTime criadoEm;
}