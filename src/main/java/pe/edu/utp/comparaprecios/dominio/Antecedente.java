package pe.edu.utp.comparaprecios.dominio;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * Proceso de contratacion normalizado a partir del registro OCDS de la OECE,
 * junto con el resultado de la evaluacion de comparabilidad.
 */
public class Antecedente {

    // Datos de la fuente
    private String ocid;
    private String nomenclatura;
    private String descripcionProceso;
    private String entidad;
    private String metodo;
    private LocalDate fechaPublicacion;
    private LocalDate fechaAdjudicacion;
    private BigDecimal valorEstimado;
    private BigDecimal montoAdjudicado;
    private String proveedor;
    private int numeroAdjudicaciones;
    private int numeroDocumentos;
    private String urlFuente;
    private List<ItemContratado> items = new ArrayList<>();

    // Resultado del analisis
    private EstadoComparabilidad estado;
    private List<String> motivos = new ArrayList<>();
    private List<String> modelos = new ArrayList<>();
    private BigDecimal cantidad;
    private BigDecimal precioUnitario;
    private AjusteTemporal ajuste;

    public void agregarMotivo(EstadoComparabilidad nuevoEstado, String motivo) {
        this.estado = (this.estado == null) ? nuevoEstado : this.estado.peor(nuevoEstado);
        this.motivos.add(motivo);
    }

    public String getOcid() { return ocid; }
    public void setOcid(String ocid) { this.ocid = ocid; }

    public String getNomenclatura() { return nomenclatura; }
    public void setNomenclatura(String nomenclatura) { this.nomenclatura = nomenclatura; }

    public String getDescripcionProceso() { return descripcionProceso; }
    public void setDescripcionProceso(String descripcionProceso) { this.descripcionProceso = descripcionProceso; }

    public String getEntidad() { return entidad; }
    public void setEntidad(String entidad) { this.entidad = entidad; }

    public String getMetodo() { return metodo; }
    public void setMetodo(String metodo) { this.metodo = metodo; }

    public LocalDate getFechaPublicacion() { return fechaPublicacion; }
    public void setFechaPublicacion(LocalDate fechaPublicacion) { this.fechaPublicacion = fechaPublicacion; }

    public LocalDate getFechaAdjudicacion() { return fechaAdjudicacion; }
    public void setFechaAdjudicacion(LocalDate fechaAdjudicacion) { this.fechaAdjudicacion = fechaAdjudicacion; }

    public BigDecimal getValorEstimado() { return valorEstimado; }
    public void setValorEstimado(BigDecimal valorEstimado) { this.valorEstimado = valorEstimado; }

    public BigDecimal getMontoAdjudicado() { return montoAdjudicado; }
    public void setMontoAdjudicado(BigDecimal montoAdjudicado) { this.montoAdjudicado = montoAdjudicado; }

    public String getProveedor() { return proveedor; }
    public void setProveedor(String proveedor) { this.proveedor = proveedor; }

    public int getNumeroAdjudicaciones() { return numeroAdjudicaciones; }
    public void setNumeroAdjudicaciones(int numeroAdjudicaciones) { this.numeroAdjudicaciones = numeroAdjudicaciones; }

    public int getNumeroDocumentos() { return numeroDocumentos; }
    public void setNumeroDocumentos(int numeroDocumentos) { this.numeroDocumentos = numeroDocumentos; }

    public String getUrlFuente() { return urlFuente; }
    public void setUrlFuente(String urlFuente) { this.urlFuente = urlFuente; }

    public List<ItemContratado> getItems() { return items; }
    public void setItems(List<ItemContratado> items) { this.items = items; }

    public EstadoComparabilidad getEstado() { return estado; }
    public void setEstado(EstadoComparabilidad estado) { this.estado = estado; }

    public List<String> getMotivos() { return motivos; }
    public void setMotivos(List<String> motivos) { this.motivos = motivos; }

    public List<String> getModelos() { return modelos; }
    public void setModelos(List<String> modelos) { this.modelos = modelos; }

    public BigDecimal getCantidad() { return cantidad; }
    public void setCantidad(BigDecimal cantidad) { this.cantidad = cantidad; }

    public BigDecimal getPrecioUnitario() { return precioUnitario; }
    public void setPrecioUnitario(BigDecimal precioUnitario) { this.precioUnitario = precioUnitario; }

    public AjusteTemporal getAjuste() { return ajuste; }
    public void setAjuste(AjusteTemporal ajuste) { this.ajuste = ajuste; }
}
