package pe.edu.utp.comparaprecios.persistencia;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/** Registro de cada consulta para la trazabilidad del analisis (OE6). */
@Entity
@Table(name = "consulta_realizada")
public class ConsultaRealizada {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private LocalDateTime fechaConsulta;
    private String termino;
    private String modelo;
    private Integer anioDesde;
    private Integer anioHasta;
    private int totalProcesos;
    private int utilizables;
    private int enRevision;
    private int descartados;
    private String estadoOece;
    private String estadoBcrp;
    private long duracionMs;

    @OneToMany(mappedBy = "consulta", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ResultadoClasificacion> resultados = new ArrayList<>();

    public void agregarResultado(ResultadoClasificacion r) {
        r.setConsulta(this);
        resultados.add(r);
    }

    public Long getId() { return id; }

    public LocalDateTime getFechaConsulta() { return fechaConsulta; }
    public void setFechaConsulta(LocalDateTime fechaConsulta) { this.fechaConsulta = fechaConsulta; }

    public String getTermino() { return termino; }
    public void setTermino(String termino) { this.termino = termino; }

    public String getModelo() { return modelo; }
    public void setModelo(String modelo) { this.modelo = modelo; }

    public Integer getAnioDesde() { return anioDesde; }
    public void setAnioDesde(Integer anioDesde) { this.anioDesde = anioDesde; }

    public Integer getAnioHasta() { return anioHasta; }
    public void setAnioHasta(Integer anioHasta) { this.anioHasta = anioHasta; }

    public int getTotalProcesos() { return totalProcesos; }
    public void setTotalProcesos(int totalProcesos) { this.totalProcesos = totalProcesos; }

    public int getUtilizables() { return utilizables; }
    public void setUtilizables(int utilizables) { this.utilizables = utilizables; }

    public int getEnRevision() { return enRevision; }
    public void setEnRevision(int enRevision) { this.enRevision = enRevision; }

    public int getDescartados() { return descartados; }
    public void setDescartados(int descartados) { this.descartados = descartados; }

    public String getEstadoOece() { return estadoOece; }
    public void setEstadoOece(String estadoOece) { this.estadoOece = estadoOece; }

    public String getEstadoBcrp() { return estadoBcrp; }
    public void setEstadoBcrp(String estadoBcrp) { this.estadoBcrp = estadoBcrp; }

    public long getDuracionMs() { return duracionMs; }
    public void setDuracionMs(long duracionMs) { this.duracionMs = duracionMs; }

    @JsonIgnore
    public List<ResultadoClasificacion> getResultados() { return resultados; }
}
