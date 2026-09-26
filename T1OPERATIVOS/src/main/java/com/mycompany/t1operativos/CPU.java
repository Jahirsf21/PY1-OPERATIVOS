package com.mycompany.t1operativos;

/**
 * Representa la CPU del Mini PC.
 *
 * Mantiene el contador de programa, el registro de instrucción, el acumulador
 * y los registros de propósito general. Además, ejecuta las instrucciones
 * almacenadas en la memoria asociada.
 *
 * @author deislher sánchez funez
 */
public class CPU {
    private Memoria memoria;
    private int pc;
    private String[] ir;
    private int ac;
    private int ax;
    private int bx;
    private int cx;
    private String dx;

    /**
     * Construye una CPU asociada a la memoria indicada e inicializa sus
     * registros en cero.
     *
     * @param memoria memoria de la que se obtendrán las instrucciones.
     * @throws IllegalArgumentException si la memoria es {@code null}.
     */
    public CPU(Memoria memoria) {
        if (memoria == null) {
            throw new IllegalArgumentException("La memoria no puede ser nula.");
        }
        this.memoria = memoria;
        reiniciar();
    }

    /**
     * Busca y ejecuta la siguiente instrucción pendiente del programa.
     *
     * @return {@code true} si se ejecutó una instrucción; {@code false} si no quedan instrucciones pendientes.
     * @throws IllegalStateException si no existe una instrucción en la posición indicada por el contador de programa.
     */
    public boolean ejecutarSiguiente() {
        if (!hayInstruccionPendiente()) {
            return false;
        }
        buscarInstruccion();
        ejecutarInstruccion();
        return true;
    }

    /**
     * Ejecuta de manera consecutiva todas las instrucciones pendientes del
     * programa.
     *
     * @throws IllegalStateException si no existe una instrucción en alguna de las posiciones que debe ejecutar.
     */
    public void ejecutarTodo() {
        while(hayInstruccionPendiente()) {
            ejecutarSiguiente();
        }
    }

    /**
     * Carga en el registro de instrucción el contenido señalado por el contador
     * de programa y avanza el contador a la siguiente posición.
     *
     * @throws IllegalStateException si la posición actual no contiene una instrucción.
     */
    private void buscarInstruccion() {
        String[] instruccion = memoria.leer(pc);
        if (instruccion == null) {
            throw new IllegalStateException("No existe una instrucción en la posición " + pc + ".");
        }
        ir = instruccion.clone();
        pc++;
    }

    /**
     * Ejecuta la instrucción almacenada en el registro de instrucción.
     *
     * @throws IllegalStateException si no hay una instrucción cargada.
     * @throws IllegalArgumentException si la instrucción contiene un registro desconocido.
     */
    private void ejecutarInstruccion() {
        if (ir == null) {
            throw new IllegalStateException("No existe una instrucción cargada en el IR.");
        }
        String operador = ir[0];
        String registro = ir[1];
        switch (operador) {
            case "MOV":
                String origen = ir[2];
                if ("DX".equals(registro) && "DX".equals(origen)) {
                    break;
                }
                int valor;
                if (new Parser().validarRegistro(origen)) {
                    valor = leerRegistro(origen);
                } else {
                    valor = Integer.parseInt(origen);
                }
                escribirRegistro(registro, valor);
                break;
            case "LOAD":
                ac = leerRegistro(registro);
                break;
            case "STORE":
                escribirRegistro(registro, ac);
                break;
            case "ADD":
                ac = ac + leerRegistro(registro);
                break;
            case "SUB":
                ac = ac - leerRegistro(registro);
                break;
        }
    }

    /**
     * Lee el valor de un registro de propósito general.
     *
     * @param registro nombre del registro que se desea leer.
     * @return el valor almacenado en el registro.
     * @throws IllegalArgumentException si el registro no es reconocido.
     */
    private int leerRegistro(String registro) {
        switch (registro) {
            case "AX":
                return ax;
            case "BX":
                return bx;
            case "CX":
                return cx;
            case "DX":
                try {
                    return Integer.parseInt(dx);
                } catch (NumberFormatException e) {
                    throw new IllegalStateException("DX contiene texto y no puede usarse como número: " + dx);
                }
            default:
                throw new IllegalArgumentException("Registro desconocido: " + registro);
        }
    }

    /**
     * Almacena un valor en un registro de propósito general.
     *
     * @param registro nombre del registro que se desea modificar.
     * @param valor valor que se almacenará en el registro.
     * @throws IllegalArgumentException si el registro no es reconocido.
     */
    private void escribirRegistro(String registro, int valor) {
        switch (registro) {
            case "AX":
                ax = valor;
                break;
            case "BX":
                bx = valor;
                break;
            case "CX":
                cx = valor;
                break;
            case "DX":
                dx = Integer.toString(valor);
                break;
            default:
                throw new IllegalArgumentException("Registro desconocido: " + registro);
        }
    }

    /**
     * Comprueba si el contador de programa señala una instrucción pendiente.
     *
     * @return {@code true} si todavía queda una instrucción por ejecutar.
     */
    public boolean hayInstruccionPendiente() {
        if (memoria.getCantidadInstrucciones() == 0) {
            return false;
        }
        return pc <= memoria.getFinPrograma();
    }

    /**
     * Reinicia el contador de programa al inicio del espacio de usuario y
     * restablece todos los registros de la CPU.
     */
    public void reiniciar() {
        pc = memoria.getInicioUsuario();
        ir = null;
        ac = 0;
        ax = 0;
        bx = 0;
        cx = 0;
        dx = "";
    }

    /**
     * Obtiene la instrucción actual con formato ensamblador legible.
     *
     * @return la instrucción almacenada en el IR, o una cadena vacía si no hay
     *     una instrucción cargada.
     */
    public String getIrToString() {
        return new Parser().traducirInstruccion(ir);
    }

    /** @return el valor actual del contador de programa (PC). */
    public int getPc() {
        return pc;
    }

    /** @return la instrucción almacenada en el registro de instrucción (IR). */
    public String[] getIr() {
        if (ir == null) {
            return null;
        }
        return ir.clone();
    }

    /** @return el valor actual del acumulador (AC). */
    public int getAc() {
        return ac;
    }

    /** @return el valor actual del registro AX. */
    public int getAx() {
        return ax;
    }

    /** @return el valor actual del registro BX. */
    public int getBx() {
        return bx;
    }

    /** @return el valor actual del registro CX. */
    public int getCx() {
        return cx;
    }

    /** @return el contenido actual del registro DX. */
    public String getDx() {
        return dx;
    }

    /** Asigna texto a DX  */
    public void setDx(String valor) {
        if (valor == null) {
            throw new IllegalArgumentException("DX no puede ser nulo.");
        }
        dx = valor;
    }

}
