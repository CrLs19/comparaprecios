package pe.edu.utp.comparaprecios.dominio;

import java.math.BigDecimal;
import java.time.YearMonth;

/**
 * Precio expresado en moneda de otro periodo. Se calculan siempre los dos
 * criterios (IPC y tipo de cambio) y se informa el rango, nunca una sola cifra.
 */
public record AjusteTemporal(
        BigDecimal montoOriginal,
        YearMonth periodoOrigen,
        YearMonth periodoDestino,
        BigDecimal factorIpc,
        BigDecimal factorTipoCambio,
        BigDecimal montoSegunIpc,
        BigDecimal montoSegunTipoCambio,
        BigDecimal minimo,
        BigDecimal maximo,
        BigDecimal diferencia,
        BigDecimal diferenciaPorcentual) {
}
