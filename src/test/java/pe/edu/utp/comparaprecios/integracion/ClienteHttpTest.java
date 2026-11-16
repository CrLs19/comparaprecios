package pe.edu.utp.comparaprecios.integracion;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.*;
import org.springframework.http.*;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestTemplate;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.*;
import static org.springframework.test.web.client.response.MockRestResponseCreators.*;

class ClienteHttpTest {
    private MockRestServiceServer servidor;
    private ClienteHttp cliente;
    private static final String URL = "https://fuente.test/datos";

    @BeforeEach
    void preparar() {
        RestTemplate rest = new RestTemplate();
        servidor = MockRestServiceServer.bindTo(rest).build();
        cliente = new ClienteHttp(rest, new ObjectMapper(), 2);
    }

    @AfterEach
    void verificarSolicitudes() { servidor.verify(); }

    @Test
    void devuelveUnJsonValido() {
        servidor.expect(requestTo(URL)).andRespond(withSuccess("{\"ok\":true}", MediaType.APPLICATION_JSON));
        assertTrue(cliente.getJson("PRUEBA", URL).path("ok").asBoolean());
    }

    @Test
    void noReintentaUnError400() {
        servidor.expect(requestTo(URL)).andRespond(withStatus(HttpStatus.BAD_REQUEST));
        var e = assertThrows(ServicioExternoException.class, () -> cliente.getJson("OECE", URL));
        assertEquals("OECE", e.getFuente());
        assertTrue(e.getMessage().contains("400"));
    }

    @Test
    void recuperaElResultadoDespuesDeUnError500() {
        servidor.expect(requestTo(URL)).andRespond(withServerError());
        servidor.expect(requestTo(URL)).andRespond(withSuccess("{\"ok\":true}", MediaType.APPLICATION_JSON));
        assertTrue(cliente.getJson("BCRP", URL).path("ok").asBoolean());
    }

    @Test
    void dosReintentosPermitenTresIntentosEnTotal() {
        for (int i = 0; i < 3; i++) servidor.expect(requestTo(URL)).andRespond(withServerError());
        var e = assertThrows(ServicioExternoException.class, () -> cliente.getJson("BCRP", URL));
        assertTrue(e.getMessage().contains("3 intentos"));
    }

    @Test
    void noAceptaHtmlComoRespuestaJson() {
        servidor.expect(requestTo(URL)).andRespond(withSuccess("<html>Error</html>", MediaType.TEXT_HTML));
        var e = assertThrows(ServicioExternoException.class, () -> cliente.getJson("OECE", URL));
        assertTrue(e.getMessage().contains("JSON"));
    }
}
