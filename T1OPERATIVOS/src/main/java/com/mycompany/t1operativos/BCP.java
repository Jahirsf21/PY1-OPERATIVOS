package com.mycompany.t1operativos;

import java.time.LocalDateTime;

/**
 * Representa el Bloque de Control de Proceso (BCP) de un programa.
 *
 * Almacena la identificación, el estado, la prioridad, los límites de memoria
 * y el contexto de ejecución necesario para administrar un proceso.
 *
 * @author deislher sánchez funez
 */
public class BCP {
    private int idProceso;
    private String estado;
    private int prioridad;
    private int pc;
    private int inicioMemoria;
    private int finMemoria;
    private String[] ir;
    private int ac;
    private int ax;
    private int bx;
    private int cx;
    private String dx;
    private int[] pila;
    private int punteroPila;
    private int cpuActual;
    private LocalDateTime tiempoInicio;
    private LocalDateTime tiempoFinal;
    private long tiempoEmpleadoSegundos;
    private int direccionSiguienteBCP;

    /**
     * Construye un BCP para un proceso nuevo e inicializa sus registros en cero.
     *
     * @param idProceso identificador único del proceso.
     * @param prioridad prioridad asignada al proceso.
     * @param inicioMemoria primera posición de memoria asignada al proceso.
     * @param finMemoria última posición de memoria asignada al proceso.
     */
    public BCP(int idProceso, int prioridad, int inicioMemoria, int finMemoria) {
        this.idProceso = idProceso;
        this.estado = "NUEVO";
        this.prioridad = prioridad;
        this.pc = inicioMemoria;
        this.inicioMemoria = inicioMemoria;
        this.finMemoria = finMemoria;
        this.ir = null;
        this.ac = 0;
        this.ax = 0;
        this.bx = 0;
        this.cx = 0;
        this.dx = "";
        this.pila = new int[5];
        this.punteroPila = -1;
        this.cpuActual = -1;
        this.tiempoInicio = null;
        this.tiempoFinal = null;
        this.tiempoEmpleadoSegundos = 0;
        this.direccionSiguienteBCP = -1;
    }

    /**
     * Guarda en el BCP el contexto actual de una CPU, incluidos el contador de
     * programa, el registro de instrucción, el acumulador y los registros de
     * propósito general.
     *
     * @param cpu CPU cuyo contexto se desea guardar.
     * @throws IllegalArgumentException si la CPU es {@code null}.
     */
    public void guardarContexto(CPU cpu) {
        if (cpu == null) {
            throw new IllegalArgumentException("El cpu no puede ser nulo.");
        }
        this.pc = cpu.getPc();
        String[] instruccionActual = cpu.getIr();
        if (instruccionActual == null) {
            this.ir = null;
        } else {
            this.ir = instruccionActual.clone();
        }
        this.ac = cpu.getAc();
        this.ax = cpu.getAx();
        this.bx = cpu.getBx();
        this.cx = cpu.getCx();
        this.dx = cpu.getDx();
    }

    /** Guarda un valor en la pila del proceso. */
    public void apilar(int valor) {
        if (punteroPila >= pila.length - 1) {
            throw new IllegalStateException("Desbordamiento de pila: capacidad máxima de 5 valores.");
        }
        pila[++punteroPila] = valor;
    }

    /** Retira el último valor de la pila del proceso. */
    public int desapilar() {
        if (punteroPila < 0) {
            throw new IllegalStateException("La pila está vacía.");
        }
        int valor = pila[punteroPila];
        pila[punteroPila--] = 0;
        return valor;
    }

    /** @return una copia de los valores presentes en la pila. */
    public int[] getPila() {
        int[] valores = new int[punteroPila + 1];
        System.arraycopy(pila, 0, valores, 0, punteroPila + 1);
        return valores;
    }

    /** @return la cantidad de valores presentes en la pila. */
    public int getCantidadEnPila() {
        return punteroPila + 1;
    }

    /** @return la posición del último valor en la pila. */
    public int getPunteroPila() {
        return punteroPila;
    }

    /** @return la capacidad máxima de la pila. */
    public int getCapacidadPila() {
        return pila.length;
    }

    /** Asigna el identificador de la CPU en la que se ejecuta el proceso. */
    public void setCpuActual(int cpuActual) {
        if (cpuActual < 0) {
            throw new IllegalArgumentException("El identificador de CPU no puede ser negativo.");
        }
        this.cpuActual = cpuActual;
    }

    /** @return el identificador de CPU, o -1 si aún no se ha asignado. */
    public int getCpuActual() {
        return cpuActual;
    }

    /** Registra un segundo simulado de uso de CPU. */
    public void aumentarTiempoEmpleado() {
        tiempoEmpleadoSegundos++;
    }

    /** @return el tiempo simulado de CPU consumido, en segundos. */
    public long getTiempoEmpleadoSegundos() {
        return tiempoEmpleadoSegundos;
    }

    /** @return la fecha y hora de la primera ejecución, o null si aún no inició. */
    public LocalDateTime getTiempoInicio() {
        return tiempoInicio;
    }

    /** @return la fecha y hora de finalización, o null si aún no terminó. */
    public LocalDateTime getTiempoFinal() {
        return tiempoFinal;
    }

    /** Enlaza este BCP con la dirección en memoria del siguiente BCP. */
    public void setDireccionSiguienteBCP(int direccion) {
        if (direccion < -1) {
            throw new IllegalArgumentException("La dirección del siguiente BCP no es válida.");
        }
        direccionSiguienteBCP = direccion;
    }

    /** @return la dirección del siguiente BCP, o -1 si no hay otro. */
    public int getDireccionSiguienteBCP() {
        return direccionSiguienteBCP;
    }

    /**
     * Obtiene la instrucción guardada en el IR con formato ensamblador legible.
     *
     * @return la instrucción almacenada en el IR, o una cadena vacía si no hay
     *     una instrucción guardada.
     */
    public String getIrToString() {
        return new Parser().traducirInstruccion(ir);
    }


    /** Establece el estado del proceso como {@code NUEVO}. */
    public void setEstadoNuevo() {
        this.estado = "NUEVO";
    }


    /** Establece el estado del proceso como {@code LISTO}. */
    public void setEstadoListo() {
        this.estado = "LISTO";
    }

    /** Establece el estado como EJECUTANDO y registra el primer inicio. */
    public void setEstadoEjecutando() {
        this.estado = "EJECUTANDO";
        if (tiempoInicio == null) {
            tiempoInicio = LocalDateTime.now();
        }
    }

    /** Establece el estado del proceso como EN_ESPERA. */
    public void setEstadoBloqueado() {
        this.estado = "EN_ESPERA";
    }

    /** Establece el estado del proceso como SUSPENDIDO. */
    public void setEstadoSuspendido() {
        this.estado = "SUSPENDIDO";
    }

    /** Establece el estado del proceso como LISTO_SUSPENDIDO. */
    public void setEstadoListoSuspendido() {
        this.estado = "LISTO_SUSPENDIDO";
    }

    /** Establece el estado como TERMINADO y registra su finalización. */
    public void setEstadoTerminado() {
        this.estado = "TERMINADO";
        if (tiempoFinal == null) {
            tiempoFinal = LocalDateTime.now();
        }
    }

    /** @return el identificador del proceso. */
    public int getIdProceso() {
        return idProceso;
    }
    
    /** @return el estado actual del proceso. */
    public String getEstado() {
        return estado;
    }

    /** @return la prioridad asignada al proceso. */
    public int getPrioridad() {
        return prioridad;
    }

    /** @return el valor guardado del contador de programa (PC). */
    public int getPc() {
        return pc;
    }

    /** @return la primera posición de memoria asignada al proceso. */
    public int getInicioMemoria() {
        return inicioMemoria;
    }

    /** @return la última posición de memoria asignada al proceso. */
    public int getFinMemoria() {
        return finMemoria;
    }

    /** @return la dirección base del programa en memoria. */
    public int getBase() {
        return inicioMemoria;
    }

    /** @return el tamaño del programa, incluyendo ambas direcciones límite. */
    public int getAlcance() {
        return finMemoria - inicioMemoria + 1;
    }

    /**
     * Obtiene una copia de la instrucción guardada en el IR.
     *
     * @return una copia de la instrucción, o {@code null} si no hay una
     *     instrucción guardada.
     */
    public String[] getIr() {
        if (ir == null) {
            return null;
        }
        return ir.clone();
    }

    /** @return el valor guardado del acumulador (AC). */
    public int getAc() {
        return ac;
    }
    
    /** @return el valor guardado del registro AX. */
    public int getAx() {
        return ax;
    }
    
    /** @return el valor guardado del registro BX. */
    public int getBx() {
        return bx;
    }

    /** @return el valor guardado del registro CX. */
    public int getCx() {
        return cx;
    }
    
    /** @return el contenido guardado del registro DX. */
    public String getDx() {
        return dx;
    }
    
}
