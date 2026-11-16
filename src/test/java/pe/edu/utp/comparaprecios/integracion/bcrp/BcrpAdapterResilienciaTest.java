package pe.edu.utp.comparaprecios.integracion.bcrp;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import pe.edu.utp.comparaprecios.integracion.ServicioExternoException;
import java.time.YearMonth;
import static org.junit.jupiter.api.Assertions.*;

class BcrpAdapterResilienciaTest {
    private final ObjectMapper mapper = new ObjectMapper();

    @Test
    void datoNoDisponibleNoSeConvierteEnCeroNiEnUltimoPeriodoComun() throws Exception {
        var s = BcrpAdapter.interpretar(mapper.readTree("""
            {"config":{"series":[{"name":"IPC"},{"name":"Tipo de cambio"}]},
             "periods":[{"name":"Jul.2024","values":["114","3.7"]},
                        {"name":"Ago.2024","values":["n.d.","3.8"]}]}
            """));
        assertTrue(s.ipc(YearMonth.of(2024, 8)).isEmpty());
        assertEquals(YearMonth.of(2024, 7), s.ultimoPeriodoComun().orElseThrow());
    }

    @Test
    void noAdivinaLasColumnasSiNoEstanIdentificadas() throws Exception {
        var raiz = mapper.readTree("{\"config\":{\"series\":[{\"name\":\"desconocido\"}]}}");
        assertThrows(ServicioExternoException.class, () -> BcrpAdapter.interpretar(raiz));
    }

    @Test
    void periodoMalformadoMantieneElErrorComoFalloDeLaFuente() {
        var e = assertThrows(ServicioExternoException.class, () -> BcrpAdapter.periodo("Jul.error"));
        assertEquals("BCRP", e.getFuente());
    }
}
