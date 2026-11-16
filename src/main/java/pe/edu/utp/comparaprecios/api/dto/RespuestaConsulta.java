package pe.edu.utp.comparaprecios.api.dto;

import pe.edu.utp.comparaprecios.dominio.Antecedente;

import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.List;

/** Respuesta uniforme que el orquestador entrega al dashboard. */
public record RespuestaConsulta(
        Long idConsulta,
        LocalDateTime fechaConsulta,
        String termino,
        String modelo,
        Integer anioDesde,
        Integer anioHasta,
        int encontradosEnOece,
        int analizados,
        long utilizables,
        long enRevision,
        long descartados,
        YearMonth periodoReferencia,
        String estadoOece,
        String estadoBcrp,
        List<String> advertencias,
        long duracionMs,
        List<Antecedente> antecedentes) {
}
