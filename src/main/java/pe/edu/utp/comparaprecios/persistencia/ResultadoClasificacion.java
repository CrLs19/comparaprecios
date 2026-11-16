package pe.edu.utp.comparaprecios.persistencia;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import pe.edu.utp.comparaprecios.dominio.EstadoComparabilidad;

import java.math.BigDecimal;

/** Estado y motivo con que se clasifico cada proceso en una consulta. */
@Entity
@Table(name = "resultado_clasificacion")
public class ResultadoClasificacion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @JsonIgnore
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "consulta_id")
    private ConsultaRealizada consulta;

    private String ocid;

    @Enumerated(EnumType.STRING)
    private EstadoComparabilidad estado;

    @Column(length = 1000)
    private String motivos;

    private BigDecimal precioUnitario;

    public ResultadoClasificacion() {
    }

    public ResultadoClasificacion(String ocid, EstadoComparabilidad estado, String motivos, BigDecimal precioUnitario) {
        this.ocid = ocid;
        this.estado = estado;
        this.motivos = motivos;
        this.precioUnitario = precioUnitario;
    }

    public Long getId() { return id; }
    public ConsultaRealizada getConsulta() { return consulta; }
    public void setConsulta(ConsultaRealizada consulta) { this.consulta = consulta; }
    public String getOcid() { return ocid; }
    public EstadoComparabilidad getEstado() { return estado; }
    public String getMotivos() { return motivos; }
    public BigDecimal getPrecioUnitario() { return precioUnitario; }
}
