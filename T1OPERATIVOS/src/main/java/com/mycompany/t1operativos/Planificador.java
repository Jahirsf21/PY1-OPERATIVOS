package com.mycompany.t1operativos;

import java.util.ArrayList;
import java.util.List;

/**
 * Selecciona el primer proceso listo.
 *
 * @author deislher sánchez funez
 */
public class Planificador {
    private List<Proceso> colaListos;

    /**
     * Construye una cola vacía de procesos listos para CPU.
     */
    public Planificador() {
        colaListos = new ArrayList<>();
    }

    /**
     * Añade un proceso al final de listos.
     *
     * @param proceso proceso listo y cargado en RAM.
     */
    public void agregarListo(Proceso proceso) {
        if (proceso == null) {
            throw new IllegalArgumentException("El proceso no puede ser nulo.");
        }
        BCP bcp = proceso.getBCP();
        if (!"LISTO".equals(bcp.getEstado())) {
            throw new IllegalStateException("Solo se agregan procesos listos.");
        }
        for (Proceso listo : colaListos) {
            if (listo.getBCP().getIdProceso() == bcp.getIdProceso()) {
                throw new IllegalStateException("El proceso ya está en la cola de listos.");
            }
        }
        colaListos.add(proceso);
    }

    /**
     * Retira el primer proceso listo.
     *
     * @return proceso seleccionado, o null.
     */
    public Proceso obtenerSiguiente() {
        if (colaListos.isEmpty()) {
            return null;
        }
        Proceso proceso = colaListos.get(0);
        BCP bcp = proceso.getBCP();
        if (!"LISTO".equals(bcp.getEstado())) {
            throw new IllegalStateException("El primer proceso debe estar listo.");
        }
        return colaListos.remove(0);
    }

    /**
     * Comprueba si hay procesos listos.
     *
     * @return true si la cola no está vacía.
     */
    public boolean hayProcesosListos() {
        return !colaListos.isEmpty();
    }

    /**
     * Obtiene la cola de procesos listos para CPU.
     *
     * @return cola de procesos listos.
     */
    public List<Proceso> getListos() {
        return colaListos;
    }

    /**
     * Vacía la cola de listos.
     */
    public void limpiar() {
        colaListos.clear();
    }
}
