// Dashboard: llama solo a la API propia (/api/v1), nunca a las APIs del Estado.
const API = '/api/v1';
let ultimaRespuesta = null;

const soles = v => v == null ? '—' :
    Number(v).toLocaleString('es-PE', {minimumFractionDigits: 2, maximumFractionDigits: 2});

const badge = estado => ({
    UTILIZABLE: '<span class="badge bg-success">Utilizable</span>',
    REVISION: '<span class="badge bg-warning text-dark">Revisión</span>',
    DESCARTADO: '<span class="badge bg-danger">Descartado</span>'
}[estado] || estado);

function mostrar(id, visible) {
    document.getElementById(id).classList.toggle('d-none', !visible);
}

async function pedir(url) {
    const r = await fetch(url);
    const datos = await r.json();
    if (!r.ok) {
        throw new Error(datos.mensaje || ('Error ' + r.status));
    }
    return datos;
}

document.getElementById('formBusqueda').addEventListener('submit', async e => {
    e.preventDefault();
    const params = new URLSearchParams({termino: document.getElementById('termino').value.trim()});
    ['modelo', 'desde', 'hasta'].forEach(c => {
        const v = document.getElementById(c).value.trim();
        if (v) params.append(c, v);
    });

    mostrar('error', false);
    mostrar('advertencias', false);
    mostrar('cargando', true);
    document.getElementById('btnBuscar').disabled = true;
    try {
        ultimaRespuesta = await pedir(`${API}/antecedentes?${params}`);
        pintarResumen(ultimaRespuesta);
        pintarTabla();
    } catch (err) {
        document.getElementById('error').textContent = err.message;
        mostrar('error', true);
    } finally {
        mostrar('cargando', false);
        document.getElementById('btnBuscar').disabled = false;
    }
});

document.getElementById('filtroEstado').addEventListener('change', pintarTabla);

function pintarResumen(r) {
    document.getElementById('nAnalizados').textContent = r.analizados;
    document.getElementById('nUtil').textContent = r.utilizables;
    document.getElementById('nRev').textContent = r.enRevision;
    document.getElementById('nDesc').textContent = r.descartados;
    const fecha = new Date(r.fechaConsulta).toLocaleString('es-PE');
    document.getElementById('infoConsulta').textContent =
        `· consulta n.º ${r.idConsulta} del ${fecha} · ${r.encontradosEnOece} procesos en la OECE` +
        (r.periodoReferencia ? ` · ajuste a ${r.periodoReferencia}` : '');
    const adv = document.getElementById('advertencias');
    adv.innerHTML = (r.advertencias || []).map(a => `<div>${a}</div>`).join('');
    mostrar('advertencias', r.advertencias && r.advertencias.length > 0);
    mostrar('resumen', true);
    mostrar('panelResultados', true);
}

function pintarTabla() {
    if (!ultimaRespuesta) return;
    const filtro = document.getElementById('filtroEstado').value;
    const filas = ultimaRespuesta.antecedentes
        .filter(a => !filtro || a.estado === filtro)
        .map(a => {
            const aj = a.ajuste;
            const item = a.items.length === 1 ? a.items[0] : null;
            return `<tr>
                <td>${badge(a.estado)}</td>
                <td class="entidad"><div>${a.entidad || ''}</div><small class="text-muted">${a.nomenclatura || a.ocid}</small></td>
                <td class="text-nowrap">${a.fechaPublicacion || ''}</td>
                <td>${(a.modelos || []).join(', ') || '—'}</td>
                <td class="num">${item && item.cantidad != null ? item.cantidad : a.items.length + ' ítems'}</td>
                <td class="num">${soles(a.montoAdjudicado)}</td>
                <td class="num fw-semibold">${soles(a.precioUnitario)}</td>
                <td class="num">${aj ? soles(aj.montoSegunIpc) : '—'}</td>
                <td class="num">${aj ? soles(aj.montoSegunTipoCambio) : '—'}</td>
                <td class="motivo">${a.motivos.join('<br>')}</td>
                <td><a href="${a.urlFuente}" target="_blank" rel="noopener" title="Registro original en la OECE">fuente</a></td>
            </tr>`;
        });
    document.getElementById('tablaResultados').innerHTML =
        filas.join('') || '<tr><td colspan="11" class="text-center text-muted">Sin resultados</td></tr>';
}

document.getElementById('formAjuste').addEventListener('submit', async e => {
    e.preventDefault();
    const params = new URLSearchParams({
        monto: document.getElementById('monto').value,
        origen: document.getElementById('origen').value
    });
    const destino = document.getElementById('destino').value;
    if (destino) params.append('destino', destino);
    const caja = document.getElementById('resultadoAjuste');
    try {
        const a = await pedir(`${API}/ajustes?${params}`);
        caja.innerHTML = `
            <table class="table table-sm w-auto mb-1">
                <tr><th>Criterio</th><th class="text-end">Factor</th><th class="text-end">Monto ${a.periodoDestino}</th></tr>
                <tr><td>IPC Lima Metropolitana</td><td class="num">${a.factorIpc}</td><td class="num">S/ ${soles(a.montoSegunIpc)}</td></tr>
                <tr><td>Tipo de cambio</td><td class="num">${a.factorTipoCambio}</td><td class="num">S/ ${soles(a.montoSegunTipoCambio)}</td></tr>
            </table>
            <div class="small">Rango de referencia: S/ ${soles(a.minimo)} – S/ ${soles(a.maximo)}
            (diferencia S/ ${soles(a.diferencia)}, ${a.diferenciaPorcentual} %)</div>`;
    } catch (err) {
        caja.innerHTML = `<div class="alert alert-danger mb-0">${err.message}</div>`;
    }
});
