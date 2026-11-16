package pe.edu.utp.comparaprecios.negocio;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import pe.edu.utp.comparaprecios.dominio.AjusteTemporal;
import pe.edu.utp.comparaprecios.dominio.SeriesEconomicas;
import pe.edu.utp.comparaprecios.integracion.bcrp.BcrpAdapter;

import java.io.InputStream;
import java.math.BigDecimal;
import java.time.YearMonth;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Usa la respuesta real de BCRPData (consultada el 12/10/2026) para reproducir
 * el caso de la Municipalidad de Ate descrito en el planteamiento del problema.
 */
class CalculadoraAjusteTest {

    private SeriesEconomicas series() throws Exception {
        try (InputStream in = getClass().getResourceAsStream("/bcrp_ipc_tc_2024-07_2026-08.json")) {
            return BcrpAdapter.interpretar(new ObjectMapper().readTree(in));
        }
    }

    @Test
    void identificaLasSeriesPorNombreAunqueVenganEnOtroOrden() throws Exception {
        SeriesEconomicas s = series();
        assertEquals(new BigDecimal("113.998099"), s.ipc(YearMonth.of(2024, 7)).orElseThrow());
        assertEquals(new BigDecimal("3.764135"), s.tipoCambio(YearMonth.of(2024, 7)).orElseThrow());
        assertEquals(YearMonth.of(2026, 8), s.ultimoPeriodoComun().orElseThrow());
    }

    @Test
    void casoAteJulio2024AJunio2026() throws Exception {
        AjusteTemporal a = new CalculadoraAjuste().ajustar(new BigDecimal("551.89"),
                YearMonth.of(2024, 7), YearMonth.of(2026, 6), series());
        assertEquals(new BigDecimal("582.36"), a.montoSegunIpc());
        assertEquals(new BigDecimal("499.38"), a.montoSegunTipoCambio());
        assertEquals(new BigDecimal("82.98"), a.diferencia());
        assertEquals(new BigDecimal("16.6"), a.diferenciaPorcentual());
    }
}
