package pe.edu.utp.comparaprecios.negocio;

import org.springframework.stereotype.Component;
import pe.edu.utp.comparaprecios.dominio.AjusteTemporal;
import pe.edu.utp.comparaprecios.dominio.SeriesEconomicas;

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
import java.time.YearMonth;

/**
 * Expresa un precio historico en moneda de otro periodo con dos criterios:
 *   IPC:  monto x IPC(destino) / IPC(origen)
 *   TC:   monto x TC(destino)  / TC(origen)
 * El resultado es un rango; la diferencia porcentual se calcula sobre el minimo.
 */
@Component
public class CalculadoraAjuste {

    private static final MathContext MC = new MathContext(12, RoundingMode.HALF_UP);

    public AjusteTemporal ajustar(BigDecimal monto, YearMonth origen, YearMonth destino, SeriesEconomicas series) {
        BigDecimal ipcOrigen = series.ipc(origen).orElseThrow(() -> sinDato("IPC", origen));
        BigDecimal ipcDestino = series.ipc(destino).orElseThrow(() -> sinDato("IPC", destino));
        BigDecimal tcOrigen = series.tipoCambio(origen).orElseThrow(() -> sinDato("tipo de cambio", origen));
        BigDecimal tcDestino = series.tipoCambio(destino).orElseThrow(() -> sinDato("tipo de cambio", destino));

        validarIndice("IPC", origen, ipcOrigen);
        validarIndice("IPC", destino, ipcDestino);
        validarIndice("tipo de cambio", origen, tcOrigen);
        validarIndice("tipo de cambio", destino, tcDestino);

        BigDecimal factorIpc = ipcDestino.divide(ipcOrigen, MC);
        BigDecimal factorTc = tcDestino.divide(tcOrigen, MC);

        BigDecimal segunIpc = monto.multiply(factorIpc).setScale(2, RoundingMode.HALF_UP);
        BigDecimal segunTc = monto.multiply(factorTc).setScale(2, RoundingMode.HALF_UP);
        BigDecimal minimo = segunIpc.min(segunTc);
        BigDecimal maximo = segunIpc.max(segunTc);
        BigDecimal diferencia = maximo.subtract(minimo);
        BigDecimal porcentaje = minimo.signum() == 0 ? BigDecimal.ZERO
                : diferencia.multiply(BigDecimal.valueOf(100)).divide(minimo, 1, RoundingMode.HALF_UP);

        return new AjusteTemporal(monto, origen, destino,
                factorIpc.setScale(6, RoundingMode.HALF_UP), factorTc.setScale(6, RoundingMode.HALF_UP),
                segunIpc, segunTc, minimo, maximo, diferencia, porcentaje);
    }

    private static IllegalArgumentException sinDato(String serie, YearMonth periodo) {
        return new IllegalArgumentException("El BCRP no publica " + serie + " para " + periodo);
    }

    private static void validarIndice(String serie, YearMonth periodo, BigDecimal valor) {
        if (valor.signum() <= 0) {
            throw new IllegalArgumentException("El valor de " + serie + " para " + periodo + " debe ser positivo");
        }
    }
}
