package pe.edu.utp.comparaprecios.integracion;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

/**
 * GET con reintentos controlados. Los errores 4xx no se reintentan porque
 * indican una peticion mal formada, no una caida del servicio.
 */
@Component
public class ClienteHttp {

    private static final Logger log = LoggerFactory.getLogger(ClienteHttp.class);

    private final RestTemplate restTemplate;
    private final ObjectMapper mapper;
    private final int reintentos;

    public ClienteHttp(RestTemplate restTemplate, ObjectMapper mapper,
                       @Value("${http.reintentos}") int reintentos) {
        this.restTemplate = restTemplate;
        this.mapper = mapper;
        this.reintentos = reintentos;
    }

    public JsonNode getJson(String fuente, String url) {
        RestClientException ultimoError = null;
        for (int intento = 1; intento <= reintentos + 1; intento++) {
            try {
                String cuerpo = restTemplate.getForObject(url, String.class);
                return mapper.readTree(cuerpo);
            } catch (HttpClientErrorException e) {
                throw new ServicioExternoException(fuente, "Peticion rechazada (" + e.getStatusCode() + ")", e);
            } catch (RestClientException e) {
                ultimoError = e;
                log.warn("{}: intento {} fallido para {} - {}", fuente, intento, url, e.getMessage());
                esperar(500L * intento);
            } catch (Exception e) {
                throw new ServicioExternoException(fuente, "La respuesta no es un JSON valido", e);
            }
        }
        throw new ServicioExternoException(fuente, "Servicio no disponible tras " + (reintentos + 1) + " intentos", ultimoError);
    }

    private static void esperar(long ms) {
        try {
            Thread.sleep(ms);
        } catch (InterruptedException ie) {
            Thread.currentThread().interrupt();
        }
    }
}
