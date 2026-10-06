package com.mycompany.t1operativos;

/**
 * Clase que valida, procesa y traduce las instrucciones ensamblador de la Mini PC.
 *
 * Reconoce las instrucciones del proyecto y los registros AX, BX, CX, DX, AH y AL.
 *
 * @author deislher sánchez funez
 */
public class Parser {

    /**
     * Convierte una instrucción simple a un arreglo de tres elementos.
     * El valor de una instrucción simple se establece en cero.
     *
     * @param instruccion arreglo que contiene la operación y el operando.
     * @return un arreglo con el operador, el operando y el valor {@code "0"}.
     */
    public String[] procesarInstruccionSimple(String[] instruccion) {
        String[] resultado = new String[3];
        String[] partesOperacion = instruccion[0].replaceAll("\\s+", " ").split(" ");
        resultado[0] = partesOperacion[0];
        resultado[1] = partesOperacion[1];
        resultado[2] = "0";
        return resultado;
    }

    /**
     * Convierte una instrucción de asignación a un arreglo de tres
     * elementos.
     *
     * @param instruccion arreglo que contiene la operación, el registro y el valor de la asignación.
     * @return un arreglo con el operador, el registro y el valor.
     */
    public String[] procesarInstruccionAsignacion(String[] instruccion) {
        String[] resultado = new String[3];
        String[] partesOperacion = instruccion[0].replaceAll("\\s+", " ").split(" ");
        resultado[0] = partesOperacion[0];
        resultado[1] = partesOperacion[1];
        resultado[2] = instruccion[1].trim();
        return resultado;
    }

    /**
     * Convierte INC o DEC sin registro a un arreglo de tres elementos.
     *
     * @param instruccion arreglo que contiene la operación.
     * @return un arreglo con el operador, un operando vacío y el valor {@code "0"}.
     */
    public String[] procesarInstruccionSinOperando(String[] instruccion) {
        return new String[]{instruccion[0].trim(), "", "0"};
    }

    /**
     * Convierte PARAM a un arreglo con el operador seguido de sus valores.
     *
     * @param instruccion partes de la instrucción separadas por comas.
     * @return un arreglo con el operador y los parámetros.
     */
    public String[] procesarInstruccionParametros(String[] instruccion) {
        String[] partesOperacion = instruccion[0].trim().replaceAll("\\s+", " ").split(" ");
        String[] resultado = new String[instruccion.length + 1];
        resultado[0] = partesOperacion[0];
        resultado[1] = partesOperacion[1];
        for (int i = 1; i < instruccion.length; i++) {
            resultado[i + 1] = instruccion[i].trim();
        }
        return resultado;
    }

    /**
     * Valida el formato, el operador y el registro de una instrucción simple.
     *
     * @param instruccion partes de la instrucción que se desea validar.
     * @return {@code null} si la instrucción es válida; en caso contrario, un mensaje con la causa del error.
     */
    public String validarInstruccionSimple(String[] instruccion) {
        if (instruccion.length != 1) {
            return "Formato inválido: una instrucción simple debe tener el formato \"OPERADOR REGISTRO\".";
        }
        String[] partesOperacion = instruccion[0].replaceAll("\\s+", " ").split(" ");
        if (partesOperacion.length != 2) {
            return "Formato inválido: se esperaba \"OPERADOR REGISTRO\".";
        }
        String operador = partesOperacion[0];
        String registro = partesOperacion[1];
        if (!validarOperadorSimple(operador)) {
            return "Operador desconocido: \"" + operador + "\". Operadores válidos: LOAD, STORE, ADD, SUB.";
        }
        if (!validarRegistro(registro)) {
            return "Registro inválido: \"" + registro + "\". Registros válidos: AX, BX, CX, DX.";
        }
        return null;
    }

    /**
     * Valida MOV según su registro destino.
     *
     * @param instruccion partes de la instrucción que se desea validar.
     * @return {@code null} si la instrucción es válida; en caso contrario, un mensaje con la causa del error.
     */
    public String validarInstruccionAsignacion(String[] instruccion) {
        if (instruccion.length != 2) {
            return "Formato inválido: una instrucción de asignación debe tener el formato \"OPERADOR REGISTRO, VALOR\".";
        }
        String[] partesOperacion = instruccion[0].replaceAll("\\s+", " ").split(" ");
        if (partesOperacion.length != 2) {
            return "Formato inválido: se esperaba \"OPERADOR REGISTRO, VALOR\".";
        }
        String operador = partesOperacion[0];
        String registro = partesOperacion[1];
        if (!operador.equals("MOV")) {
            if (validarOperadorSimple(operador)) {
                return "El operador \"" + operador + "\" no admite valor.";
            } else {
                return "Operador desconocido: \"" + operador + "\". Operador válido: MOV.";
            }
        }
        String valorTexto = instruccion[1].trim();
        if ("AH".equals(registro)) {
            switch (valorTexto) {
                case "3CH":
                case "3DH":
                case "4DH":
                case "40H":
                case "41H":
                    return null;
                default:
                    return "AH solo admite 3CH, 3DH, 4DH, 40H y 41H.";
            }
        }
        if ("AL".equals(registro)) {
            if ("DX".equals(valorTexto) || "AL".equals(valorTexto)) {
                return null;
            }
            return validarAsignacionTexto(valorTexto);
        }
        if (!validarRegistro(registro)) {
            return "Registro inválido: \"" + registro + "\". Registros válidos: AX, BX, CX, DX, AH, AL.";
        }
        if ("DX".equals(registro)) {
            if ("AL".equals(valorTexto)) {
                return null;
            }
            if (valorTexto.contains("\"")) {
                return validarAsignacionTexto(valorTexto);
            }
        }
        if (validarRegistro(valorTexto)) {
            return null;
        }
        int valor;
        try {
            valor = Integer.parseInt(valorTexto);
        } catch (NumberFormatException e) {
            if ("DX".equals(registro)) {
                return "El origen \"" + valorTexto + "\" debe ser un registro (AX, BX, CX, DX, AL), un entero o texto entre comillas dobles.";
            }
            return "El origen \"" + valorTexto + "\" debe ser un registro (AX, BX, CX, DX) o un número entero válido.";
        }
        if (valor < -127 || valor > 127) {
            return "El valor " + valor + " está fuera del rango permitido (-127 a 127).";
        }
        return null;
    }

    /**
     * Valida las comillas de una cadena.
     *
     * @param valor texto del operando.
     * @return null si es válido, o el mensaje de error.
     */
    private String validarAsignacionTexto(String valor) {
        if (valor.length() < 2 || !valor.startsWith("\"") || !valor.endsWith("\"")) {
            return "El texto debe estar entre comillas dobles.";
        }
        if (valor.indexOf('"', 1) != valor.length() - 1) {
            return "El texto no admite comillas interiores.";
        }
        return null;
    }

    /**
     * Valida INC o DEC con un registro opcional.
     *
     * @param instruccion partes de la instrucción que se desea validar.
     * @return {@code null} si la instrucción es válida; en caso contrario, un mensaje con la causa del error.
     */
    public String validarInstruccionIncremento(String[] instruccion) {
        if (instruccion.length != 1) {
            return "Formato inválido: INC y DEC no admiten comas.";
        }
        String[] partes = instruccion[0].trim().replaceAll("\\s+", " ").split(" ");
        if (partes.length > 2) {
            return "Formato inválido: se esperaba \"INC [REGISTRO]\" o \"DEC [REGISTRO]\".";
        }
        if (partes.length == 2 && !validarRegistro(partes[1])) {
            return "Registro inválido: \"" + partes[1] + "\". Registros válidos: AX, BX, CX, DX.";
        }
        return null;
    }

    /**
     * Valida SWAP o CMP con dos registros separados por coma.
     *
     * @param instruccion partes de la instrucción que se desea validar.
     * @return {@code null} si la instrucción es válida; en caso contrario, un mensaje con la causa del error.
     */
    public String validarInstruccionDosRegistros(String[] instruccion) {
        if (instruccion.length != 2) {
            return "Formato inválido: se esperaba \"OPERADOR REGISTRO1, REGISTRO2\".";
        }
        String[] partes = instruccion[0].trim().replaceAll("\\s+", " ").split(" ");
        if (partes.length != 2) {
            return "Formato inválido: se esperaba \"OPERADOR REGISTRO1, REGISTRO2\".";
        }
        if (!validarRegistro(partes[1])) {
            return "Registro inválido: \"" + partes[1] + "\". Registros válidos: AX, BX, CX, DX.";
        }
        String segundo = instruccion[1].trim();
        if (!validarRegistro(segundo)) {
            return "Registro inválido: \"" + segundo + "\". Registros válidos: AX, BX, CX, DX.";
        }
        return null;
    }

    /**
     * Valida el formato y el código de una interrupción.
     *
     * @param instruccion partes de la instrucción que se desea validar.
     * @return {@code null} si la instrucción es válida; en caso contrario, un mensaje con la causa del error.
     */
    public String validarInstruccionInterrupcion(String[] instruccion) {
        if (instruccion.length != 1) {
            return "Formato inválido: se esperaba \"INT CÓDIGO\".";
        }
        String[] partes = instruccion[0].trim().replaceAll("\\s+", " ").split(" ");
        if (partes.length != 2) {
            return "Formato inválido: se esperaba \"INT CÓDIGO\".";
        }
        if (!validarInterrupcion(partes[1])) {
            return "Interrupción inválida: \"" + partes[1] + "\". Válidas: 09H, 10H, 20H, 21H.";
        }
        return null;
    }

    /**
     * Valida un desplazamiento entero para JMP, JE o JNE.
     *
     * @param instruccion partes de la instrucción que se desea validar.
     * @return {@code null} si la instrucción es válida; en caso contrario, un mensaje con la causa del error.
     */
    public String validarInstruccionSalto(String[] instruccion) {
        if (instruccion.length != 1) {
            return "Formato inválido: se esperaba \"OPERADOR DESPLAZAMIENTO\".";
        }
        String[] partes = instruccion[0].trim().replaceAll("\\s+", " ").split(" ");
        if (partes.length != 2) {
            return "Formato inválido: se esperaba \"OPERADOR DESPLAZAMIENTO\".";
        }
        try {
            Integer.parseInt(partes[1]);
        } catch (NumberFormatException e) {
            return "Desplazamiento inválido: \"" + partes[1] + "\". Se esperaba un número entero.";
        }
        return null;
    }

    /**
     * Valida de uno a tres parámetros numéricos separados por comas.
     *
     * @param instruccion partes de la instrucción que se desea validar.
     * @return {@code null} si la instrucción es válida; en caso contrario, un mensaje con la causa del error.
     */
    public String validarInstruccionParametros(String[] instruccion) {
        if (instruccion.length > 3) {
            return "Formato inválido: PARAM admite como máximo tres valores.";
        }
        String[] partes = instruccion[0].trim().replaceAll("\\s+", " ").split(" ");
        if (partes.length != 2) {
            return "Formato inválido: se esperaba \"PARAM 1\", \"PARAM 1, 2\" o \"PARAM 1, 2, 3\".";
        }
        try {
            Integer.parseInt(partes[1]);
        } catch (NumberFormatException e) {
            return "Parámetro inválido: \"" + partes[1] + "\". Se esperaba un número entero.";
        }
        for (int i = 1; i < instruccion.length; i++) {
            String valor = instruccion[i].trim();
            try {
                Integer.parseInt(valor);
            } catch (NumberFormatException e) {
                return "Parámetro inválido: \"" + valor + "\". Se esperaba un número entero.";
            }
        }
        return null;
    }

    /**
     * Comprueba que PUSH o POP tengan exactamente un registro.
     *
     * @param instruccion partes de la instrucción que se desea validar.
     * @return {@code null} si la instrucción es válida; en caso contrario, un mensaje con la causa del error.
     */
    public String validarInstruccionPila(String[] instruccion) {
        if (instruccion.length != 1) {
            return "Formato inválido: se esperaba \"PUSH REGISTRO\" o \"POP REGISTRO\".";
        }
        String[] partes = instruccion[0].trim().replaceAll("\\s+", " ").split(" ");
        if (partes.length != 2) {
            return "Formato inválido: se esperaba \"OPERADOR REGISTRO\".";
        }
        if (!validarRegistro(partes[1])) {
            return "Registro inválido: \"" + partes[1] + "\". Registros válidos: AX, BX, CX, DX.";
        }
        return null;
    }

    /**
     * Comprueba si un código de interrupción es reconocido.
     *
     * @param interrupcion código de interrupción que se desea comprobar.
     * @return {@code true} si el código es 09H, 10H, 20H o 21H.
     */
    public boolean validarInterrupcion(String interrupcion) {
        switch (interrupcion) {
            case "09H":
            case "10H":
            case "20H":
            case "21H":
                return true;
            default:
                return false;
        }
    }

    /**
     * Comprueba si un operador corresponde a una instrucción sin valor.
     *
     * @param operador operador que se desea comprobar.
     * @return {@code true} si el operador es LOAD, STORE, SUB o ADD.
     */
    public boolean validarOperadorSimple(String operador) {
        switch (operador) {
            case "LOAD":
            case "STORE":
            case "SUB":
            case "ADD":
                return true;
            default:
                return false;
        }
    }

    /**
     * Comprueba si un operador es reconocido por la Mini PC.
     *
     * @param operador operador que se desea comprobar.
     * @return {@code true} si el operador es válido.
     */
    public boolean validarOperador(String operador) {
        switch (operador) {
            case "LOAD":
            case "STORE":
            case "MOV":
            case "SUB":
            case "ADD":
            case "INC":
            case "DEC":
            case "SWAP":
            case "INT":
            case "JMP":
            case "CMP":
            case "JE":
            case "JNE":
            case "PARAM":
            case "PUSH":
            case "POP":
                return true;
            default:
                return false;
        }
    }

    /**
     * Comprueba un registro de las operaciones generales.
     *
     * @param registro registro que se desea comprobar.
     * @return {@code true} si el registro es AX, BX, CX o DX.
     */
    public boolean validarRegistro(String registro) {
        switch (registro) {
            case "AX":
            case "BX":
            case "CX":
            case "DX":
                return true;
            default:
                return false;
        }
    }

    /**
     * Separa, valida y procesa una línea de código ensamblador.
     *
     * @param instruccion línea de código que se desea procesar.
     * @return el resultado con la instrucción procesada o un mensaje de error.
     */
    public ResultadoParser procesarInstruccion(String instruccion) {
        String[] partes = instruccion.trim().split(",", 2);
        String[] partesOperacion = partes[0].trim().replaceAll("\\s+", " ").split(" ");
        String operador = partesOperacion[0];

        if (!validarOperador(operador)) {
            return new ResultadoParser(false, null, "Operador desconocido: \"" + operador + "\".");
        }
        if (!"MOV".equals(operador)) {
            partes = instruccion.trim().split(",", -1);
        }
        String error;
        switch (operador) {
            case "MOV":
                error = validarInstruccionAsignacion(partes);
                if (error == null) {
                    return new ResultadoParser(true, procesarInstruccionAsignacion(partes), null);
                }
                return new ResultadoParser(false, null, error);
            case "INC":
            case "DEC":
                error = validarInstruccionIncremento(partes);
                if (error == null) {
                    if (partesOperacion.length == 1) {
                        return new ResultadoParser(true, procesarInstruccionSinOperando(partes), null);
                    }
                    return new ResultadoParser(true, procesarInstruccionSimple(partes), null);
                }
                return new ResultadoParser(false, null, error);
            case "SWAP":
            case "CMP":
                error = validarInstruccionDosRegistros(partes);
                if (error == null) {
                    return new ResultadoParser(true, procesarInstruccionAsignacion(partes), null);
                } else {
                    return new ResultadoParser(false, null, error);
                }
            case "INT":
                error = validarInstruccionInterrupcion(partes);
                if (error == null) {
                    return new ResultadoParser(true, procesarInstruccionSimple(partes), null);
                } else {
                    return new ResultadoParser(false, null, error);
                }
            case "JMP":
            case "JE":
            case "JNE":
                error = validarInstruccionSalto(partes);
                if (error == null) {
                    return new ResultadoParser(true, procesarInstruccionSimple(partes), null);
                } else {
                    return new ResultadoParser(false, null, error);
                }
            case "PARAM":
                error = validarInstruccionParametros(partes);
                if (error == null) {
                    return new ResultadoParser(true, procesarInstruccionParametros(partes), null);
                } else {
                    return new ResultadoParser(false, null, error);
                }
            case "PUSH":
            case "POP":
                error = validarInstruccionPila(partes);
                if (error == null) {
                    return new ResultadoParser(true, procesarInstruccionSimple(partes), null);
                } else {
                    return new ResultadoParser(false, null, error);
                }
            default:
                break;
        }
        if (partes.length == 1) {
            error = validarInstruccionSimple(partes);
            if (error == null) {
                return new ResultadoParser(true, procesarInstruccionSimple(partes), null);
            }
            return new ResultadoParser(false, null, error);
        }
        if (partes.length == 2) {
            error = validarInstruccionAsignacion(partes);
            if (error == null) {
                return new ResultadoParser(true, procesarInstruccionAsignacion(partes), null);
            }
            return new ResultadoParser(false, null, error);
        }
        return new ResultadoParser(false, null, "Formato inválido: se esperaba \"OPERADOR REGISTRO\" o \"OPERADOR REGISTRO, VALOR\".");
    }

    /**
     * Traduce una instrucción procesada a su representación en ensamblador.
     *
     * @param instruccion arreglo con el operador y sus operandos.
     * @return la instrucción en ensamblador, o una cadena vacía si no hay una instrucción.
     */
    public String traducirInstruccion(String[] instruccion) {
        if (instruccion == null) {
            return "";
        }
        String operador = instruccion[0];
        String registro = instruccion[1];
        if (("INC".equals(operador) || "DEC".equals(operador)) && registro.isEmpty()) {
            return operador;
        }
        if ("MOV".equals(operador) || "SWAP".equals(operador) || "CMP".equals(operador)) {
            String valor = instruccion[2];
            return operador + " " + registro + ", " + valor;
        }
        if ("PARAM".equals(operador)) {
            String resultado = operador + " " + registro;
            for (int i = 2; i < instruccion.length; i++) {
                resultado += ", " + instruccion[i];
            }
            return resultado;
        }
        return operador + " " + registro;
    }
}
