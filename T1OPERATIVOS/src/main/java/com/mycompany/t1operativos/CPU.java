package com.mycompany.t1operativos;

/**
 * Representa la CPU de la Mini PC.
 *
 * Mantiene los registros y ejecuta la instrucción que recibe. La lectura de
 * instrucciones desde memoria corresponde al controlador.
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

    /** Construye una CPU e inicializa sus registros. */
    public CPU() {
        reiniciar();
    }

    /** Ejecuta una instrucción validada por el parser y cargada por el controlador. */
    public void ejecutarInstruccion(String[] instruccion) {
        if (instruccion == null || instruccion.length == 0) {
            throw new IllegalArgumentException("La instrucción no puede ser nula ni vacía.");
        }
        ir = instruccion.clone();
        String operador = ir[0];
        switch (operador) {
            case "MOV":
                String registro = ir[1];
                String origen = ir[2];
                if ("DX".equals(registro) && "DX".equals(origen)) {
                    return;
                }
                int valor = esRegistro(origen) ? leerRegistro(origen) : Integer.parseInt(origen);
                escribirRegistro(registro, valor);
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
            default:
                throw new IllegalArgumentException("La instrucción " + operador + " todavía no está implementada en la CPU.");
        }
    }

    /** Avanza el contador de programa a la siguiente posición. */
    public void avanzarPc() {
        pc++;
    }

    /** Lee el valor de un registro. */
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

    /** Almacena un valor en un registro. */
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

    /** Comprueba si un valor corresponde a un registro. */
    private boolean esRegistro(String valor) {
        return "AX".equals(valor) || "BX".equals(valor) || "CX".equals(valor) || "DX".equals(valor);
    }

    /** Reinicia los registros de la CPU. */
    public void reiniciar() {
        pc = 0;
        ir = null;
        ac = 0;
        ax = 0;
        bx = 0;
        cx = 0;
        dx = "";
    }

    /** @return el valor actual del contador de programa. */
    public int getPc() {
        return pc;
    }

    /** Cambia el valor del contador de programa. */
    public void setPc(int valor) {
        if (valor < 0) {
            throw new IllegalArgumentException("El PC no puede ser negativo.");
        }
        pc = valor;
    }

    /** @return la instrucción actual. */
    public String[] getIr() {
        return ir == null ? null : ir.clone();
    }

    /** @return el valor actual del acumulador. */
    public int getAc() {
        return ac;
    }

    /** @return el valor actual de AX. */
    public int getAx() {
        return ax;
    }

    /** @return el valor actual de BX. */
    public int getBx() {
        return bx;
    }

    /** @return el valor actual de CX. */
    public int getCx() {
        return cx;
    }

    /** @return el contenido actual de DX. */
    public String getDx() {
        return dx;
    }

    /** Asigna texto a DX. */
    public void setDx(String valor) {
        if (valor == null) {
            throw new IllegalArgumentException("DX no puede ser nulo.");
        }
        dx = valor;
    }
}
