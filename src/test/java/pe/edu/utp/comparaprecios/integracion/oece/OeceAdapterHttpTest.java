package pe.edu.utp.comparaprecios.integracion.oece;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.*;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestTemplate;
import pe.edu.utp.comparaprecios.integracion.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.*;
import static org.springframework.test.web.client.response.MockRestResponseCreators.*;

class OeceAdapterHttpTest {
    private MockRestServiceServer servidor;
    private OeceAdapter adaptador;
    private final String base = "https://oece.test";

    @BeforeEach
    void preparar() {
        RestTemplate rest = new RestTemplate();
        servidor = MockRestServiceServer.bindTo(rest).build();
        adaptador = new OeceAdapter(new ClienteHttp(rest, new ObjectMapper(), 0), base, "seace_v3", 3);
    }

    @AfterEach
    void verificar() { servidor.verify(); }

    @Test
    void recorrePaginasYRespetaElFinDeLaBusqueda() {
        for (int i = 1; i <= 2; i++) {
            servidor.expect(requestTo(base + "/api/v1/search?search=TONER&source=seace_v3&format=json&page=" + i))
                    .andRespond(withSuccess("{\"results\":[{\"compiledRelease\":{\"ocid\":\"ocds-test-" + i
                            + "\",\"tender\":{\"datePublished\":\"2024-07-01T00:00:00-05:00\"}}}],"
                            + "\"pagination\":{\"has_next\":" + (i == 1) + "}}", MediaType.APPLICATION_JSON));
        }
        var resultados = adaptador.buscar("TONER");
        assertEquals(2, resultados.size());
        assertEquals("ocds-test-2", resultados.get(1).ocid());
    }

    @Test
    void respuestaSinResultsNoEsUnaBusquedaVaciaExitosa() {
        servidor.expect(requestTo(base + "/api/v1/search?search=TONER&source=seace_v3&format=json&page=1"))
                .andRespond(withSuccess("{}", MediaType.APPLICATION_JSON));
        assertThrows(ServicioExternoException.class, () -> adaptador.buscar("TONER"));
    }

    @Test
    void detalleSinCompiledReleaseInformaFalloDeLaFuente() {
        servidor.expect(requestTo(base + "/api/v1/record/ocds-test-1?format=json"))
                .andRespond(withSuccess("{\"records\":[]}", MediaType.APPLICATION_JSON));
        assertThrows(ServicioExternoException.class, () -> adaptador.obtenerDetalle("ocds-test-1"));
    }
}
