package pe.edu.utp.comparaprecios.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Configuration
public class HttpConfig {

    @Bean
    public RestTemplate restTemplate(@Value("${http.connect-timeout}") int connectTimeout,
                                     @Value("${http.read-timeout}") int readTimeout) {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(connectTimeout);
        factory.setReadTimeout(readTimeout);

        RestTemplate rt = new RestTemplate(factory);
        // El portal de la OECE rechaza peticiones sin cabeceras de navegador
        rt.getInterceptors().add((request, body, execution) -> {
            request.getHeaders().set("Accept", "application/json");
            request.getHeaders().set("User-Agent", "Mozilla/5.0 (ComparaPrecios-UTP/0.1)");
            return execution.execute(request, body);
        });
        return rt;
    }

    /** Hilos para consultar en paralelo el detalle de cada proceso. */
    @Bean(destroyMethod = "shutdown")
    public ExecutorService ejecutorDetalle(@Value("${oece.hilos-detalle}") int hilos) {
        return Executors.newFixedThreadPool(hilos);
    }
}
