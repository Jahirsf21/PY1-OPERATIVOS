package com.mycompany.t1operativos;

/**
 * Representa la CPU de la Mini PC.
 *
 * Mantiene los registros y ejecuta la instrucción que recibe. La lectura de
 * instrucciones desde memoria corresponde al gestor de procesos.
 *
 * @author deislher sánchez funez
 */
public class CPU {
    private int pc;
    private String[] ir;
    private int ac;
    private int ax;
    private int bx;
    private int cx;
    private String dx;
    private int ah;
    private int al;
    private boolean flag;

    /**
     * Construye una CPU e inicializa sus registros.
     */
    public CPU() {
        reiniciar();
    }

    /**
     * Carga los registros del contexto.
     *
     * @param pc contador de programa.
     * @param ir instrucción guardada, o null.
     * @param ac acumulador.
     * @param ax registro AX.
     * @param bx registro BX.
     * @param cx registro CX.
     * @param dx registro DX.
     * @param ah registro AH.
     * @param al registro AL.
     * @param flag resultado de la comparación.
     */
    public void cargarContexto(int pc, String[] ir, int ac, int ax, int bx, int cx, String dx, int ah, int al, boolean flag) {
        this.pc = pc;
        this.ir = ir;
        this.ac = ac;
        this.ax = ax;
        this.bx = bx;
        this.cx = cx;
        this.dx = dx;
        this.ah = ah;
        this.al = al;
        this.flag = flag;
    }

    /**
     * Carga en el IR la instrucción que se va a ejecutar.
     *
     * @param instruccion arreglo con el operador y sus operandos.
     */
    public void cargarInstruccion(String[] instruccion) {
        if (instruccion == null || instruccion.length == 0) {
            throw new IllegalArgumentException("La instrucción no puede ser nula ni vacía.");
        }
        ir = instruccion;
    }

    /**
     * Ejecuta una instrucción validada por el parser.
     *
     * @param instruccion arreglo con la instrucción que se desea ejecutar.
     */
    public void ejecutarInstruccion(String[] instruccion) {
        cargarInstruccion(instruccion);
        String operador = ir[0];
        switch (operador) {
            case "MOV":
                ejecutarMov(ir[1], ir[2]);
                break;
            case "LOAD":
                ac = leerRegistro(ir[1]);
                break;
            case "STORE":
                escribirRegistro(ir[1], ac);
                break;
            case "ADD":
                ac = ac + leerRegistro(ir[1]);
                break;
            case "SUB":
                ac = ac - leerRegistro(ir[1]);
                break;
            case "INC":
                if (ir[1].isEmpty()) {
                    ac++;
                } else {
                    escribirRegistro(ir[1], leerRegistro(ir[1]) + 1);
                }
                break;
            case "DEC":
                if (ir[1].isEmpty()) {
                    ac--;
                } else {
                    escribirRegistro(ir[1], leerRegistro(ir[1]) - 1);
                }
                break;
            case "SWAP":
                int primerValor = leerRegistro(ir[1]);
                int segundoValor = leerRegistro(ir[2]);
                escribirRegistro(ir[1], segundoValor);
                escribirRegistro(ir[2], primerValor);
                break;
            case "CMP":
                flag = leerRegistro(ir[1]) == leerRegistro(ir[2]);
                break;
            default:
                throw new IllegalArgumentException("La instrucción " + operador + " todavía no está implementada en la CPU.");
        }
    }

    /**
     * Ejecuta un MOV validado por el parser.
     *
     * @param registro registro destino.
     * @param origen registro o valor de origen.
     */
    private void ejecutarMov(String registro, String origen) {
        if ("AH".equals(registro)) {
            switch (origen) {
                case "3CH":
                    ah = 60;
                    break;
                case "3DH":
                    ah = 61;
                    break;
                case "4DH":
                    ah = 77;
                    break;
                case "40H":
                    ah = 64;
                    break;
                case "41H":
                    ah = 65;
                    break;
            }
            return;
        }
        if ("AL".equals(registro)) {
            if (esRegistro(origen)) {
                al = leerRegistro(origen);
            } else if (!"AL".equals(origen)) {
                al = Integer.parseInt(origen);
            }
            return;
        }
        if ("DX".equals(registro)) {
            if ("AL".equals(origen)) {
                dx = Integer.toString(al);
                return;
            }
            if ("DX".equals(origen)) {
                return;
            }
            if (origen.startsWith("\"")) {
                dx = origen.substring(1, origen.length() - 1);
                return;
            }
        }
        int valor;
        if (esRegistro(origen)) {
            valor = leerRegistro(origen);
        } else {
            valor = Integer.parseInt(origen);
        }
        escribirRegistro(registro, valor);
    }

    /**
     * Avanza el contador de programa a la siguiente posición.
     */
    public void avanzarPc() {
        pc++;
    }

    /**
     * Lee el valor numérico de un registro.
     *
     * @param registro nombre del registro que se desea leer.
     * @return el valor del registro.
     */
    public int leerRegistro(String registro) {
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
     * Almacena un valor numérico en un registro.
     *
     * @param registro nombre del registro que se desea actualizar.
     * @param valor valor que se desea almacenar.
     */
    public void escribirRegistro(String registro, int valor) {
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
     * Comprueba si un valor corresponde a un registro.
     *
     * @param valor nombre que se desea comprobar.
     * @return {@code true} si el valor es AX, BX, CX o DX.
     */
    private boolean esRegistro(String valor) {
        return "AX".equals(valor) || "BX".equals(valor) || "CX".equals(valor) || "DX".equals(valor);
    }

    /**
     * Reinicia los registros de la CPU.
     */
    public void reiniciar() {
        pc = 0;
        ir = null;
        ac = 0;
        ax = 0;
        bx = 0;
        cx = 0;
        dx = "";
        ah = 0;
        al = 0;
        flag = false;
    }

    /**
     * Obtiene el contador de programa actual.
     *
     * @return el valor del PC.
     */
    public int getPc() {
        return pc;
    }

    /**
     * Cambia el valor del contador de programa.
     *
     * @param valor posición de memoria que se desea asignar al PC.
     */
    public void setPc(int valor) {
        if (valor < 0) {
            throw new IllegalArgumentException("El PC no puede ser negativo.");
        }
        pc = valor;
    }

    /**
     * Obtiene la instrucción actual.
     *
     * @return el IR, o {@code null} si no hay una instrucción.
     */
    public String[] getIr() {
        return ir;
    }

    /**
     * Obtiene el valor actual del acumulador.
     *
     * @return el valor de AC.
     */
    public int getAc() {
        return ac;
    }

    /**
     * Obtiene el valor actual del registro AX.
     *
     * @return el valor de AX.
     */
    public int getAx() {
        return ax;
    }

    /**
     * Obtiene el valor actual del registro BX.
     *
     * @return el valor de BX.
     */
    public int getBx() {
        return bx;
    }

    /**
     * Obtiene el valor actual del registro CX.
     *
     * @return el valor de CX.
     */
    public int getCx() {
        return cx;
    }

    /**
     * Obtiene el contenido actual del registro DX.
     *
     * @return el contenido de DX.
     */
    public String getDx() {
        return dx;
    }

    /**
     * Obtiene el código de servicio de AH.
     *
     * @return el valor de AH.
     */
    public int getAh() {
        return ah;
    }

    /**
     * Obtiene el valor de AL.
     *
     * @return el valor de AL.
     */
    public int getAl() {
        return al;
    }

    /**
     * Comprueba el resultado de la última comparación.
     *
     * @return {@code true} si los registros comparados eran iguales.
     */
    public boolean esIgual() {
        return flag;
    }

    /**
     * Asigna texto al registro DX.
     *
     * @param valor texto que se desea almacenar.
     */
    public void setDx(String valor) {
        if (valor == null) {
            throw new IllegalArgumentException("DX no puede ser nulo.");
        }
        dx = valor;
    }

    /**
     * Asigna un entero a AL.
     *
     * @param valor entero que se guardará.
     */
    public void setAl(int valor) {
        al = valor;
    }
}
