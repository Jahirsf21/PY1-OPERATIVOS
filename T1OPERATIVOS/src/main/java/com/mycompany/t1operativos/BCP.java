package com.mycompany.t1operativos;

import java.time.LocalDateTime;

/**
 * Bloque de control de proceso.
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
    private boolean flag;
    private int pila1;
    private int pila2;
    private int pila3;
    private int pila4;
    private int pila5;
    private int punteroPila;
    private int cpuActual;
    private LocalDateTime tiempoInicio;
    private LocalDateTime tiempoFinal;
    private long tiempoEmpleadoSegundos;
    private int direccionSiguienteBCP;

    /**
     * Construye el BCP de un proceso.
     *
     * @param idProceso identificador del proceso.
     * @param prioridad prioridad asignada al proceso.
     * @param inicioMemoria primera posición de memoria asignada.
     * @param finMemoria última posición de memoria asignada.
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
        this.flag = false;
        this.pila1 = 0;
        this.pila2 = 0;
        this.pila3 = 0;
        this.pila4 = 0;
        this.pila5 = 0;
        this.punteroPila = -1;
        this.cpuActual = -1;
        this.tiempoInicio = null;
        this.tiempoFinal = null;
        this.tiempoEmpleadoSegundos = 0;
        this.direccionSiguienteBCP = -1;
    }

    /**
     * Guarda el contexto actual de la CPU.
     *
     * @param cpu CPU cuyo contexto se desea guardar.
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
        this.flag = cpu.esIgual();
    }

    /**
     * Guarda un valor en la pila del proceso.
     *
     * @param valor valor que se desea apilar.
     */
    public void apilar(int valor) {
        if (punteroPila >= getCapacidadPila() - 1) {
            throw new IllegalStateException("Desbordamiento de pila: capacidad máxima de 5 valores.");
        }
        setValorPila(punteroPila + 1, valor);
        punteroPila++;
    }

    /**
     * Retira el último valor de la pila del proceso.
     *
     * @return el valor retirado de la pila.
     */
    public int desapilar() {
        if (punteroPila < 0) {
            throw new IllegalStateException("La pila está vacía.");
        }
        int valor = getValorPila(punteroPila);
        setValorPila(punteroPila, 0);
        punteroPila--;
        return valor;
    }

    /**
     * Obtiene el valor del atributo de una posición de la pila.
     *
     * @param posicion índice de la posición, de 0 a 4.
     * @return el valor almacenado en esa posición.
     */
    public int getValorPila(int posicion) {
        switch (posicion) {
            case 0:
                return pila1;
            case 1:
                return pila2;
            case 2:
                return pila3;
            case 3:
                return pila4;
            case 4:
                return pila5;
            default:
                throw new IllegalArgumentException("Posición de pila inválida: " + posicion);
        }
    }

    /**
     * Actualiza el atributo de una posición de la pila.
     *
     * @param posicion índice de la posición, de 0 a 4.
     * @param valor valor que se desea almacenar.
     */
    private void setValorPila(int posicion, int valor) {
        switch (posicion) {
            case 0:
                pila1 = valor;
                break;
            case 1:
                pila2 = valor;
                break;
            case 2:
                pila3 = valor;
                break;
            case 3:
                pila4 = valor;
                break;
            case 4:
                pila5 = valor;
                break;
            default:
                throw new IllegalArgumentException("Posición de pila inválida: " + posicion);
        }
    }

    /**
     * Reúne los valores ocupados de la pila sin modificar sus atributos.
     *
     * @return un arreglo nuevo con los valores desde el fondo al tope.
     */
    public int[] getPila() {
        int[] valores = new int[punteroPila + 1];
        for (int i = 0; i < valores.length; i++) {
            valores[i] = getValorPila(i);
        }
        return valores;
    }

    /**
     * Obtiene la cantidad de valores almacenados en la pila.
     *
     * @return la cantidad de valores presentes.
     */
    public int getCantidadEnPila() {
        return punteroPila + 1;
    }

    /**
     * Obtiene la posición del último valor de la pila.
     *
     * @return la posición del tope, o {@code -1} si la pila está vacía.
     */
    public int getPunteroPila() {
        return punteroPila;
    }

    /**
     * Obtiene la capacidad máxima de la pila.
     *
     * @return la cantidad máxima de valores permitidos.
     */
    public int getCapacidadPila() {
        return 5;
    }

    /**
     * Asigna la CPU en la que se ejecuta el proceso.
     *
     * @param cpuActual identificador de la CPU.
     */
    public void setCpuActual(int cpuActual) {
        if (cpuActual < 0) {
            throw new IllegalArgumentException("El identificador de CPU no puede ser negativo.");
        }
        this.cpuActual = cpuActual;
    }

    /**
     * Obtiene el identificador de la CPU asignada.
     *
     * @return el identificador de CPU, o {@code -1} si no está asignada.
     */
    public int getCpuActual() {
        return cpuActual;
    }

    /**
     * Incrementa el tiempo empleado en un segundo.
     */
    public void aumentarTiempoEmpleado() {
        aumentarTiempoEmpleado(1);
    }

    /**
     * Acumula segundos en el tiempo empleado.
     *
     * @param segundos cantidad de segundos que se desea sumar.
     */
    public void aumentarTiempoEmpleado(long segundos) {
        if (segundos < 0) {
            throw new IllegalArgumentException("El tiempo empleado no puede ser negativo.");
        }
        tiempoEmpleadoSegundos += segundos;
    }

    /**
     * Obtiene el tiempo empleado por el proceso.
     *
     * @return el tiempo empleado en segundos.
     */
    public long getTiempoEmpleadoSegundos() {
        return tiempoEmpleadoSegundos;
    }

    /**
     * Obtiene la duración total del proceso.
     *
     * @return la duración en segundos.
     */
    public long getTiempoTotalSegundos() {
        return tiempoEmpleadoSegundos;
    }

    /**
     * Obtiene la fecha de inicio del proceso.
     *
     * @return la fecha de inicio, o {@code null} si aún no inició.
     */
    public LocalDateTime getTiempoInicio() {
        return tiempoInicio;
    }

    /**
     * Obtiene la fecha de finalización del proceso.
     *
     * @return la fecha de finalización, o {@code null} si aún no terminó.
     */
    public LocalDateTime getTiempoFinal() {
        return tiempoFinal;
    }

    /**
     * Asigna la dirección del siguiente BCP.
     *
     * @param direccion dirección del siguiente BCP, o {@code -1} si no hay otro.
     */
    public void setDireccionSiguienteBCP(int direccion) {
        if (direccion < -1) {
            throw new IllegalArgumentException("La dirección del siguiente BCP no es válida.");
        }
        direccionSiguienteBCP = direccion;
    }

    /**
     * Obtiene la dirección del siguiente BCP.
     *
     * @return la dirección del siguiente BCP, o {@code -1} si no hay otro.
     */
    public int getDireccionSiguienteBCP() {
        return direccionSiguienteBCP;
    }


    /**
     * Traduce la instrucción guardada en el IR a ensamblador.
     *
     * @return la instrucción en ensamblador, o una cadena vacía si no hay una instrucción.
     */
    public String getIrToString() {
        return new Parser().traducirInstruccion(ir);
    }

    /**
     * Establece el estado del proceso como NUEVO.
     */
    public void setEstadoNuevo() {
        this.estado = "NUEVO";
    }


    /**
     * Establece el estado del proceso como LISTO.
     */
    public void setEstadoListo() {
        this.estado = "LISTO";
    }

    /**
     * Establece el estado EJECUTANDO y registra el primer inicio.
     */
    public void setEstadoEjecutando() {
        setEstadoEjecutando(LocalDateTime.now());
    }

    /**
     * Establece el estado EJECUTANDO con la fecha de inicio indicada.
     *
     * @param instante fecha de inicio que se desea registrar por primera vez.
     */
    public void setEstadoEjecutando(LocalDateTime instante) {
        if (instante == null) {
            throw new IllegalArgumentException("La fecha de inicio no puede ser nula.");
        }
        this.estado = "EJECUTANDO";
        if (tiempoInicio == null) {
            tiempoInicio = instante;
        }
    }

    /**
     * Establece el estado del proceso como EN_ESPERA.
     */
    public void setEstadoBloqueado() {
        this.estado = "EN_ESPERA";
    }

    /**
     * Establece el estado del proceso como SUSPENDIDO.
     */
    public void setEstadoSuspendido() {
        this.estado = "SUSPENDIDO";
    }

    /**
     * Establece el estado del proceso como LISTO_SUSPENDIDO.
     */
    public void setEstadoListoSuspendido() {
        this.estado = "LISTO_SUSPENDIDO";
    }

    /**
     * Finaliza el proceso con la fecha calculada según su duración acumulada.
     */
    public void setEstadoTerminado() {
        if (tiempoInicio == null) {
            tiempoInicio = LocalDateTime.now();
        }
        setEstadoTerminado(tiempoInicio.plusSeconds(tiempoEmpleadoSegundos));
    }

    /**
     * Establece el estado TERMINADO y registra la primera finalización.
     *
     * @param instante fecha de finalización que se desea registrar.
     */
    public void setEstadoTerminado(LocalDateTime instante) {
        if (instante == null) {
            throw new IllegalArgumentException("La fecha de finalización no puede ser nula.");
        }
        this.estado = "TERMINADO";
        if (tiempoFinal == null) {
            tiempoFinal = instante;
        }
    }

    /**
     * Obtiene el identificador del proceso.
     *
     * @return el identificador del proceso.
     */
    public int getIdProceso() {
        return idProceso;
    }
    
    /**
     * Obtiene el estado actual del proceso.
     *
     * @return el estado del proceso.
     */
    public String getEstado() {
        return estado;
    }

    /**
     * Obtiene la prioridad asignada al proceso.
     *
     * @return la prioridad del proceso.
     */
    public int getPrioridad() {
        return prioridad;
    }

    /**
     * Obtiene el contador de programa guardado.
     *
     * @return el valor del PC.
     */
    public int getPc() {
        return pc;
    }

    /**
     * Obtiene el inicio de la memoria del proceso.
     *
     * @return la primera posición de memoria asignada.
     */
    public int getInicioMemoria() {
        return inicioMemoria;
    }

    /**
     * Obtiene el final de la memoria del proceso.
     *
     * @return la última posición de memoria asignada.
     */
    public int getFinMemoria() {
        return finMemoria;
    }

    /**
     * Obtiene la dirección base del programa.
     *
     * @return la dirección inicial del programa en memoria.
     */
    public int getBase() {
        return inicioMemoria;
    }

    /**
     * Obtiene el tamaño del programa en memoria.
     *
     * @return la cantidad de posiciones de memoria del programa.
     */
    public int getAlcance() {
        return finMemoria - inicioMemoria + 1;
    }

    /**
     * Obtiene una copia de la instrucción guardada en el IR.
     *
     * @return una copia de la instrucción, o {@code null} si no hay una instrucción.
     */
    public String[] getIr() {
        if (ir == null) {
            return null;
        }
        return ir.clone();
    }

    /**
     * Obtiene el valor guardado del acumulador.
     *
     * @return el valor de AC.
     */
    public int getAc() {
        return ac;
    }
    
    /**
     * Obtiene el valor guardado del registro AX.
     *
     * @return el valor de AX.
     */
    public int getAx() {
        return ax;
    }
    
    /**
     * Obtiene el valor guardado del registro BX.
     *
     * @return el valor de BX.
     */
    public int getBx() {
        return bx;
    }

    /**
     * Obtiene el valor guardado del registro CX.
     *
     * @return el valor de CX.
     */
    public int getCx() {
        return cx;
    }
    
    /**
     * Obtiene el contenido guardado del registro DX.
     *
     * @return el contenido de DX.
     */
    public String getDx() {
        return dx;
    }

    /**
     * Comprueba el resultado de la última comparación guardada.
     *
     * @return {@code true} si los registros comparados eran iguales.
     */
    public boolean esIgual() {
        return flag;
    }
    
}
