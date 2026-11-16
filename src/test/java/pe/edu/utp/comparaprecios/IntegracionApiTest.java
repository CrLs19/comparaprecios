package pe.edu.utp.comparaprecios;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.web.client.RestTemplate;
import pe.edu.utp.comparaprecios.persistencia.ConsultaRepository;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.response.MockRestResponseCreators.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/** Controlador, negocio, adaptadores y JPA reales; respuestas HTTP externas simuladas. */
@SpringBootTest(properties = {
    "oece.base-url=https://oece.test", "bcrp.base-url=https://bcrp.test/api",
    "http.reintentos=0", "spring.datasource.url=jdbc:h2:mem:pruebas;DB_CLOSE_DELAY=-1",
    "spring.jpa.hibernate.ddl-auto=create-drop", "spring.h2.console.enabled=false"
})
@AutoConfigureMockMvc
class IntegracionApiTest {
    @Autowired MockMvc mvc;
    @Autowired RestTemplate rest;
    @Autowired ObjectMapper mapper;
    @Autowired ConsultaRepository repositorio;
    @Autowired JdbcTemplate jdbc;
    private MockRestServiceServer servidor;

    @BeforeEach
    void preparar() {
        repositorio.deleteAll();
        servidor = MockRestServiceServer.bindTo(rest).ignoreExpectOrder(true).build();
    }

    @AfterEach
    void verificar() { servidor.verify(); }

    private String fixture(String nombre) throws Exception {
        try (var in = getClass().getResourceAsStream("/" + nombre)) {
            assertNotNull(in);
            return new String(in.readAllBytes(), StandardCharsets.UTF_8);
        }
    }

    private void buscar(String... ids) {
        var resultados = mapper.createArrayNode();
        for (String id : ids) {
            var cr = resultados.addObject().putObject("compiledRelease");
            cr.put("ocid", id).putObject("tender").put("datePublished", "2024-07-01T00:00:00-05:00");
        }
        var raiz = mapper.createObjectNode();
        raiz.set("results", resultados);
        raiz.putObject("pagination").put("has_next", false);
        servidor.expect(requestTo("https://oece.test/api/v1/search?search=TONER&source=seace_v3&format=json&page=1"))
                .andExpect(method(HttpMethod.GET)).andRespond(withSuccess(raiz.toString(), MediaType.APPLICATION_JSON));
    }

    private void detalle(String id, String descripcion, int cantidad) {
        // Datos sintéticos: no se presentan como una consulta en línea ni una contratación real.
        var raiz = mapper.createObjectNode();
        var cr = raiz.putArray("records").addObject().putObject("compiledRelease");
        cr.put("ocid", id).putObject("buyer").put("name", "Entidad de prueba");
        var tender = cr.putObject("tender");
        tender.put("title", "Proceso sintético").put("datePublished", "2024-07-01T00:00:00-05:00");
        tender.putObject("value").put("amount", 75056.50).put("currency", "PEN");
        var item = tender.putArray("items").addObject();
        item.put("description", descripcion).put("quantity", cantidad).put("statusDetails", "CONTRATADO");
        item.putObject("unit").put("name", "Unidad");
        var award = cr.putArray("awards").addObject();
        award.put("date", "2024-07-15T00:00:00-05:00");
        award.putObject("value").put("amount", 75056.50).put("currency", "PEN");
        award.putArray("suppliers").addObject().put("name", "Proveedor de prueba");
        servidor.expect(requestTo("https://oece.test/api/v1/record/" + id + "?format=json"))
                .andRespond(withSuccess(raiz.toString(), MediaType.APPLICATION_JSON));
    }

    private void bcrpDisponible() throws Exception {
        servidor.expect(requestTo(startsWith("https://bcrp.test/api/PN38705PM-PN01207PM/json/2024-7/")))
                .andRespond(withSuccess(fixture("bcrp_ipc_tc_2024-07_2026-08.json"), MediaType.APPLICATION_JSON));
    }

    private void evidencia(String nombre, MvcResult resultado) throws Exception {
        var carpeta = Path.of("target", "pruebas-api");
        Files.createDirectories(carpeta);
        var json = mapper.createObjectNode();
        json.put("tipo", "Prueba automatizada con HTTP externo simulado y H2 en memoria");
        json.put("fechaEjecucion", java.time.OffsetDateTime.now().toString());
        json.put("peticion", resultado.getRequest().getRequestURI());
        var parametros = json.putObject("parametros");
        resultado.getRequest().getParameterMap().forEach((k,v) -> parametros.put(k, String.join(",",v)));
        json.put("http", resultado.getResponse().getStatus());
        json.set("respuesta", mapper.readTree(resultado.getResponse().getContentAsString()));
        Files.writeString(carpeta.resolve(nombre + ".json"), mapper.writerWithDefaultPrettyPrinter().writeValueAsString(json));
    }

    @Test
    void busquedaCompletaClasificaAjustaYGuardaTrazabilidad() throws Exception {
        buscar("ocds-test-util", "ocds-test-revision", "ocds-test-descarte");
        detalle("ocds-test-util", "Toner TK3432", 136);
        detalle("ocds-test-revision", "Toner para impresora", 1);
        detalle("ocds-test-descarte", "Toner TK3432 y TK3182", 100);
        bcrpDisponible();
        var r = mvc.perform(get("/api/v1/antecedentes").param("termino", "TONER").param("desde", "2024").param("hasta", "2024"))
                .andExpect(status().isOk()).andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.analizados").value(3))
                .andExpect(jsonPath("$.utilizables").value(1)).andExpect(jsonPath("$.enRevision").value(1))
                .andExpect(jsonPath("$.descartados").value(1)).andExpect(jsonPath("$.estadoBcrp").value("OK"))
                .andExpect(jsonPath("$.antecedentes[?(@.estado == 'UTILIZABLE')].precioUnitario", contains(551.89)))
                .andExpect(jsonPath("$.antecedentes[?(@.estado == 'UTILIZABLE')].ajuste.montoSegunIpc", contains(584.41)))
                .andExpect(jsonPath("$.antecedentes[?(@.estado == 'UTILIZABLE')].ajuste.montoSegunTipoCambio", contains(493.07)))
                .andReturn();
        assertEquals(1, repositorio.count());
        assertEquals(3, jdbc.queryForObject("select count(*) from resultado_clasificacion", Integer.class));
        mvc.perform(get("/api/v1/consultas")).andExpect(status().isOk())
                .andExpect(jsonPath("$[0].totalProcesos").value(3)).andExpect(jsonPath("$[0].estadoBcrp").value("OK"));
        evidencia("flujo-completo", r);
    }

    @Test
    void caidaBcrpConservaPrecioOriginalYDevuelveAdvertencia() throws Exception {
        buscar("ocds-test-util"); detalle("ocds-test-util", "Toner TK3432", 136);
        servidor.expect(requestTo(startsWith("https://bcrp.test/api/"))).andRespond(withServerError());
        var r = mvc.perform(get("/api/v1/antecedentes").param("termino", "TONER"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.estadoBcrp").value("NO DISPONIBLE"))
                .andExpect(jsonPath("$.advertencias", hasSize(1)))
                .andExpect(jsonPath("$.antecedentes[0].precioUnitario").value(551.89))
                .andExpect(jsonPath("$.antecedentes[0].ajuste").isEmpty()).andReturn();
        assertEquals(1, repositorio.count());
        evidencia("caida-bcrp", r);
    }

    @Test
    void caidaBusquedaOeceDevuelve503YNoRegistraExito() throws Exception {
        servidor.expect(requestTo(startsWith("https://oece.test/api/v1/search"))).andRespond(withServerError());
        var r = mvc.perform(get("/api/v1/antecedentes").param("termino", "TONER"))
                .andExpect(status().isServiceUnavailable()).andExpect(jsonPath("$.fuente").value("OECE")).andReturn();
        assertEquals(0, repositorio.count());
        evidencia("caida-oece", r);
    }

    @Test
    void unDetalleFallidoProduceResultadoParcialConAdvertencia() throws Exception {
        buscar("ocds-test-util", "ocds-test-fallido"); detalle("ocds-test-util", "Toner TK3432", 136);
        servidor.expect(requestTo("https://oece.test/api/v1/record/ocds-test-fallido?format=json"))
                .andRespond(withStatus(HttpStatus.NOT_FOUND));
        bcrpDisponible();
        mvc.perform(get("/api/v1/antecedentes").param("termino", "TONER"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.analizados").value(1))
                .andExpect(jsonPath("$.encontradosEnOece").value(2))
                .andExpect(jsonPath("$.advertencias[0]", containsString("1 procesos")));
    }

    @Test
    void filtroModeloSeNormalizaAntesDeAjustar() throws Exception {
        buscar("ocds-test-a", "ocds-test-b");
        detalle("ocds-test-a", "Toner TK3432", 136); detalle("ocds-test-b", "Toner TK3182", 136);
        bcrpDisponible();
        mvc.perform(get("/api/v1/antecedentes").param("termino", "TONER").param("modelo", "tk-3432"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.analizados").value(1))
                .andExpect(jsonPath("$.antecedentes[0].ocid").value("ocds-test-a"));
    }

    @Test
    void anioFueraDeRangoEvitaDetalleYBcrp() throws Exception {
        buscar("ocds-test-antiguo");
        mvc.perform(get("/api/v1/antecedentes").param("termino", "TONER").param("desde", "2025"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.analizados").value(0))
                .andExpect(jsonPath("$.estadoBcrp").value("NO REQUERIDO"));
    }

    @Test
    void detalleIndividualUsaElContratoJson() throws Exception {
        detalle("ocds-test-util", "Toner TK3432", 136); bcrpDisponible();
        mvc.perform(get("/api/v1/antecedentes/ocds-test-util"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.estado").value("UTILIZABLE"))
                .andExpect(jsonPath("$.ajuste.periodoDestino").value("2026-08"));
    }

    @Test
    void ajusteDirectoRespetaLosPeriodosSolicitados() throws Exception {
        bcrpDisponible();
        mvc.perform(get("/api/v1/ajustes").param("monto", "551.89").param("origen", "2024-07").param("destino", "2026-06"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.montoSegunIpc").value(582.36))
                .andExpect(jsonPath("$.montoSegunTipoCambio").value(499.38));
    }

    @ParameterizedTest
    @ValueSource(strings = {
        "/api/v1/antecedentes", "/api/v1/antecedentes?termino=ab",
        "/api/v1/antecedentes?termino=TONER&desde=2026&hasta=2024",
        "/api/v1/antecedentes?termino=TONER&desde=2000",
        "/api/v1/antecedentes?termino=TONER&desde=texto",
        "/api/v1/antecedentes/invalido",
        "/api/v1/ajustes?monto=-1&origen=2024-07",
        "/api/v1/ajustes?monto=1&origen=no-es-mes",
        "/api/v1/ajustes?monto=1&origen=2026-07&destino=2024-07"
    })
    void parametrosInvalidosDevuelven400SinConsultarFuentes(String ruta) throws Exception {
        mvc.perform(get(ruta)).andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.estado").value(400)).andExpect(jsonPath("$.fuente").value("API"));
        assertEquals(0, repositorio.count());
    }
}
