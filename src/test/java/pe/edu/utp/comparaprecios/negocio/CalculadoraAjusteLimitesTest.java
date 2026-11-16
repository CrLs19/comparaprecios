package pe.edu.utp.comparaprecios.negocio;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import pe.edu.utp.comparaprecios.dominio.*;
import java.math.BigDecimal;
import java.time.YearMonth;
import static org.junit.jupiter.api.Assertions.*;

class CalculadoraAjusteLimitesTest {
    private final CalculadoraAjuste calculadora = new CalculadoraAjuste();
    private final YearMonth mes = YearMonth.of(2024, 7);

    private SeriesEconomicas series(String ipc, String tc) {
        SeriesEconomicas s = new SeriesEconomicas();
        s.agregarIpc(mes, new BigDecimal(ipc));
        s.agregarTipoCambio(mes, new BigDecimal(tc));
        return s;
    }

    @Test
    void elMismoPeriodoConservaElMontoOriginal() {
        AjusteTemporal a = calculadora.ajustar(new BigDecimal("551.89"), mes, mes, series("114", "3.7"));
        assertEquals(new BigDecimal("551.89"), a.montoSegunIpc());
        assertEquals(new BigDecimal("551.89"), a.montoSegunTipoCambio());
        assertEquals(0, a.diferencia().signum());
    }

    @Test
    void periodoAusenteExplicaElDatoQueFalta() {
        var e = assertThrows(IllegalArgumentException.class,
                () -> calculadora.ajustar(BigDecimal.TEN, mes, mes.plusMonths(1), series("114", "3.7")));
        assertTrue(e.getMessage().contains("2024-08"));
    }

    @ParameterizedTest
    @CsvSource({"0,3.7", "-1,3.7", "114,0", "114,-1"})
    void indicesNoPositivosSeRechazanConErrorControlado(String ipc, String tc) {
        assertThrows(IllegalArgumentException.class,
                () -> calculadora.ajustar(BigDecimal.TEN, mes, mes, series(ipc, tc)));
    }
}
