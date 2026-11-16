package pe.edu.utp.comparaprecios.negocio;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pe.edu.utp.comparaprecios.api.dto.RespuestaConsulta;
import pe.edu.utp.comparaprecios.dominio.Antecedente;
import pe.edu.utp.comparaprecios.persistencia.ConsultaRealizada;
import pe.edu.utp.comparaprecios.persistencia.ConsultaRepository;
import pe.edu.utp.comparaprecios.persistencia.ResultadoClasificacion;

import java.util.List;

/** Guarda la fecha, los parametros y el resultado de cada consulta (OE6). */
@Service
public class TrazabilidadService {

    private final ConsultaRepository repositorio;

    public TrazabilidadService(ConsultaRepository repositorio) {
        this.repositorio = repositorio;
    }

    @Transactional
    public Long registrar(RespuestaConsulta r) {
        ConsultaRealizada c = new ConsultaRealizada();
        c.setFechaConsulta(r.fechaConsulta());
        c.setTermino(r.termino());
        c.setModelo(r.modelo());
        c.setAnioDesde(r.anioDesde());
        c.setAnioHasta(r.anioHasta());
        c.setTotalProcesos(r.analizados());
        c.setUtilizables((int) r.utilizables());
        c.setEnRevision((int) r.enRevision());
        c.setDescartados((int) r.descartados());
        c.setEstadoOece(r.estadoOece());
        c.setEstadoBcrp(r.estadoBcrp());
        c.setDuracionMs(r.duracionMs());
        for (Antecedente a : r.antecedentes()) {
            c.agregarResultado(new ResultadoClasificacion(a.getOcid(), a.getEstado(),
                    String.join(" | ", a.getMotivos()), a.getPrecioUnitario()));
        }
        return repositorio.save(c).getId();
    }

    @Transactional(readOnly = true)
    public List<ConsultaRealizada> ultimas() {
        return repositorio.findTop20ByOrderByFechaConsultaDesc();
    }
}
