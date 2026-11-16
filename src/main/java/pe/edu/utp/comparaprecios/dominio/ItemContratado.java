package pe.edu.utp.comparaprecios.dominio;

import java.math.BigDecimal;

/** Item de un proceso, tal como viene en tender.items del registro OCDS. */
public class ItemContratado {

    private String descripcion;
    private String clasificacion;
    private BigDecimal cantidad;
    private String unidad;
    private BigDecimal valorTotal;
    private String estado;

    public ItemContratado() {
    }

    public ItemContratado(String descripcion, String clasificacion, BigDecimal cantidad,
                          String unidad, BigDecimal valorTotal, String estado) {
        this.descripcion = descripcion;
        this.clasificacion = clasificacion;
        this.cantidad = cantidad;
        this.unidad = unidad;
        this.valorTotal = valorTotal;
        this.estado = estado;
    }

    public String getDescripcion() { return descripcion; }
    public void setDescripcion(String descripcion) { this.descripcion = descripcion; }

    public String getClasificacion() { return clasificacion; }
    public void setClasificacion(String clasificacion) { this.clasificacion = clasificacion; }

    public BigDecimal getCantidad() { return cantidad; }
    public void setCantidad(BigDecimal cantidad) { this.cantidad = cantidad; }

    public String getUnidad() { return unidad; }
    public void setUnidad(String unidad) { this.unidad = unidad; }

    public BigDecimal getValorTotal() { return valorTotal; }
    public void setValorTotal(BigDecimal valorTotal) { this.valorTotal = valorTotal; }

    public String getEstado() { return estado; }
    public void setEstado(String estado) { this.estado = estado; }
}
