package com.mycompany.t1operativos;

import java.io.File;
import java.io.IOException;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;
import java.util.Queue;

/**
 * Coordina los procesos de la Mini PC.
 *
 * @author deislher sánchez funez
 */
public class GestorProcesos {
    private Memoria memoria;
    private Disco disco;
    private CargadorArchivos cargador;
    private Planificador planificador;
    private Despachador despachador;
    private List<Proceso> procesosRegistrados;
    private Queue<Proceso> colaTrabajos;
    private String[] instruccionPendiente;
    private int segundosPendientes;

    /**
     * Construye los recursos de la simulación.
     *
     * @param tamañoMemoria posiciones de RAM.
     * @param tamañoDisco posiciones de disco.
     */
    public GestorProcesos(int tamañoMemoria, int tamañoDisco) {
        reiniciarRecursos(tamañoMemoria, tamañoDisco);
    }

    /**
     * Carga los archivos y conserva los aceptados.
     *
     * @param archivos selección en el orden recibido.
     * @return mensajes de archivos rechazados.
     */
    public List<String> cargarProgramas(List<File> archivos) {
        if (archivos == null || archivos.isEmpty()) {
            throw new IllegalArgumentException("Debe seleccionar archivos para cargar.");
        }
        List<String> mensajes = new ArrayList<>();
        for (File archivo : archivos) {
            try {
                cargarPrograma(archivo);
            } catch (IOException | IllegalArgumentException | IllegalStateException ex) {
                String nombre = "Archivo nulo";
                if (archivo != null) {
                    nombre = archivo.getName();
                }
                mensajes.add(nombre + ": " + ex.getMessage());
            }
        }
        cargarPendientesEnMemoria();
        return mensajes;
    }

    /**
     * Avanza un paso del proceso seleccionado.
     *
     * @return salida de INT 10H, o null si no hubo salida.
     */
    public String ejecutarPaso() {
        if (!puedeEjecutar()) {
            throw new IllegalStateException("No hay procesos ejecutables.");
        }
        if (getActual() == null) {
            despachador.despachar(planificador.obtenerSiguiente());
        }
        Proceso proceso = getActual();
        BCP bcp = proceso.getBCP();
        CPU cpu = getCPU();
        try {
            if (cpu.getPc() < bcp.getInicioMemoria() || cpu.getPc() > bcp.getFinMemoria()) {
                throw new IllegalStateException("El PC está fuera de los límites del programa.");
            }
            if (instruccionPendiente == null) {
                String[] instruccion = memoria.leer(cpu.getPc());
                if (instruccion == null) {
                    throw new IllegalStateException("No existe una instrucción válida en la posición " + cpu.getPc() + ".");
                }
                cpu.cargarInstruccion(instruccion);
                if ("INT".equals(instruccion[0]) && "09H".equals(instruccion[1])) {
                    solicitarEntradaTeclado();
                    return null;
                }
                segundosPendientes = obtenerPeso(instruccion);
                instruccionPendiente = instruccion;
            }
            bcp.aumentarTiempoEmpleado();
            segundosPendientes--;
            String salida = null;
            if (segundosPendientes == 0) {
                salida = ejecutarInstruccion(instruccionPendiente);
                instruccionPendiente = null;
            }
            bcp.guardarContexto(cpu);
            if ("TERMINADO".equals(bcp.getEstado()) || !tieneInstrucciones(bcp)) {
                finalizarProceso(proceso);
            } else {
                escribirBCPEnMemoria(proceso);
            }
            return salida;
        } catch (IllegalArgumentException | IllegalStateException ex) {
            if (proceso == getActual()) {
                finalizarProceso(proceso);
            }
            throw ex;
        }
    }

    /**
     * Completa la solicitud de teclado más antigua.
     *
     * @param texto entero ingresado por el usuario.
     */
    public void recibirEntradaTeclado(String texto) {
        Proceso proceso = getSolicitudTeclado();
        if (proceso == null) {
            throw new IllegalStateException("No hay una solicitud de teclado pendiente.");
        }
        if (texto == null) {
            throw new IllegalArgumentException("Entrada inválida: se requiere un entero de 0 a 255.");
        }
        int valor;
        try {
            valor = Integer.parseInt(texto.trim());
        } catch (NumberFormatException ex) {
            throw new IllegalArgumentException("Entrada inválida: se requiere un entero de 0 a 255.");
        }
        if (valor < 0 || valor > 255) {
            throw new IllegalArgumentException("Entrada fuera de rango: el valor debe estar entre 0 y 255.");
        }
        BCP bcp = proceso.getBCP();
        long instante = System.nanoTime();
        bcp.completarEntradaTeclado(valor);
        bcp.aumentarTiempoEmpleado(proceso.finalizarEsperaTeclado(instante));
        if (tieneInstrucciones(bcp)) {
            bcp.setEstadoListo();
            planificador.agregarListo(proceso);
            escribirBCPEnMemoria(proceso);
        } else {
            finalizarProceso(proceso);
        }
    }

    /**
     * Reinicia la simulación conservando capacidades.
     */
    public void limpiar() {
        reiniciarRecursos(memoria.getTamañoTotal(), disco.getTamañoTotal());
    }

    /**
     * Obtiene la RAM compartida.
     *
     * @return memoria de la simulación.
     */
    public Memoria getMemoria() {
        return memoria;
    }

    /**
     * Obtiene el disco activo.
     *
     * @return disco de la simulación.
     */
    public Disco getDisco() {
        return disco;
    }

    /**
     * Obtiene la CPU compartida.
     *
     * @return CPU del despachador.
     */
    public CPU getCPU() {
        return despachador.getCPU();
    }

    /**
     * Obtiene el proceso que tiene CPU.
     *
     * @return proceso actual, o null.
     */
    public Proceso getActual() {
        return despachador.getActual();
    }

    /**
     * Obtiene todos los procesos registrados.
     *
     * @return lista en orden de carga, incluidos los terminados.
     */
    public List<Proceso> getProcesosRegistrados() {
        return procesosRegistrados;
    }

    /**
     * Obtiene la solicitud de teclado más antigua.
     *
     * @return solicitante, o null.
     */
    public Proceso getSolicitudTeclado() {
        Proceso solicitud = null;
        for (Proceso proceso : procesosRegistrados) {
            if ("EN_ESPERA".equals(proceso.getBCP().getEstado())) {
                if (solicitud == null || proceso.getInicioEsperaTeclado() < solicitud.getInicioEsperaTeclado()) {
                    solicitud = proceso;
                }
            }
        }
        return solicitud;
    }

    /**
     * Obtiene la instrucción pendiente.
     *
     * @return instrucción, o null.
     */
    public String[] getInstruccionPendiente() {
        return instruccionPendiente;
    }

    /**
     * Comprueba si existe un proceso ejecutable.
     *
     * @return true si hay actual o listos.
     */
    public boolean puedeEjecutar() {
        return getActual() != null || planificador.hayProcesosListos();
    }

    /**
     * Comprueba si quedan procesos sin terminar.
     *
     * @return true si algún proceso no terminó.
     */
    public boolean hayProcesosSinTerminar() {
        for (Proceso proceso : procesosRegistrados) {
            if (!"TERMINADO".equals(proceso.getBCP().getEstado())) {
                return true;
            }
        }
        return false;
    }

    /**
     * Carga y registra un programa.
     *
     * @param archivo archivo ensamblador.
     * @throws IOException si falla su lectura.
     */
    private void cargarPrograma(File archivo) throws IOException {
        if (archivo == null) {
            throw new IllegalArgumentException("Debe indicar un archivo.");
        }
        List<String[]> instrucciones = cargador.cargarArchivo(archivo.getAbsolutePath());
        List<String> errores = cargador.getErrores();
        if (!errores.isEmpty()) {
            throw new IllegalArgumentException("El archivo contiene errores:\n" + String.join("\n", errores));
        }
        if (instrucciones.size() > memoria.getTamañoTotal() - memoria.getInicioUsuario()) {
            throw new IllegalArgumentException("El programa no cabe en el espacio de usuario.");
        }
        int sinTerminar = 0;
        for (Proceso proceso : procesosRegistrados) {
            if (!"TERMINADO".equals(proceso.getBCP().getEstado())) {
                sinTerminar++;
            }
        }
        if (sinTerminar >= 5) {
            throw new IllegalStateException("Ya hay cinco procesos no terminados.");
        }
        int id = 1;
        if (!procesosRegistrados.isEmpty()) {
            id = procesosRegistrados.get(procesosRegistrados.size() - 1).getBCP().getIdProceso() + 1;
        }
        int posicionIndice = buscarEntradaLibre(disco, archivo.getName());
        int direccionDisco = buscarEspacioLibre(disco, instrucciones.size());
        Proceso proceso = new Proceso(new BCP(id, 1), posicionIndice);
        for (int i = 0; i < instrucciones.size(); i++) {
            disco.escribirDatos(direccionDisco + i, instrucciones.get(i));
        }
        disco.escribirIndice(posicionIndice, archivo.getName(), direccionDisco, instrucciones.size());
        procesosRegistrados.add(proceso);
        colaTrabajos.add(proceso);
    }

    /**
     * Busca una entrada libre sin repetir nombres.
     *
     * @param discoDestino disco activo.
     * @param nombre nombre del archivo.
     * @return primera entrada libre.
     */
    private int buscarEntradaLibre(Disco discoDestino, String nombre) {
        int libre = -1;
        for (int posicion = 0; posicion < discoDestino.getInicioDatos(); posicion++) {
            String[] entrada = discoDestino.leer(posicion);
            if (entrada == null) {
                if (libre < 0) {
                    libre = posicion;
                }
            } else if (entrada[0].equalsIgnoreCase(nombre)) {
                throw new IllegalArgumentException("El archivo " + nombre + " ya existe en el índice del disco.");
            }
        }
        if (libre < 0) {
            throw new IllegalStateException("El índice del disco no tiene entradas disponibles.");
        }
        return libre;
    }

    /**
     * Busca un bloque libre en datos de disco.
     *
     * @param discoDestino disco activo.
     * @param cantidad cantidad de instrucciones.
     * @return inicio del bloque libre.
     */
    private int buscarEspacioLibre(Disco discoDestino, int cantidad) {
        int libres = 0;
        for (int posicion = discoDestino.getInicioDatos(); posicion < discoDestino.getInicioMemoriaVirtual(); posicion++) {
            if (discoDestino.leer(posicion) == null) {
                libres++;
            } else {
                libres = 0;
            }
            if (libres == cantidad) {
                return posicion - cantidad + 1;
            }
        }
        throw new IllegalStateException("El disco no tiene un bloque de datos libre para las " + cantidad + " instrucciones del archivo.");
    }

    /**
     * Busca un bloque libre en RAM.
     *
     * @param inicio primera posición del rango.
     * @param finExclusivo límite exclusivo del rango.
     * @param cantidad posiciones requeridas.
     * @return inicio libre, o -1.
     */
    private int buscarBloqueMemoria(int inicio, int finExclusivo, int cantidad) {
        int libres = 0;
        for (int posicion = inicio; posicion < finExclusivo; posicion++) {
            if (memoria.leer(posicion) == null) {
                libres++;
            } else {
                libres = 0;
            }
            if (libres == cantidad) {
                return posicion - cantidad + 1;
            }
        }
        return -1;
    }

    /**
     * Carga los trabajos pendientes en RAM.
     */
    private void cargarPendientesEnMemoria() {
        while (!colaTrabajos.isEmpty()) {
            Proceso proceso = colaTrabajos.peek();
            BCP bcp = proceso.getBCP();
            String[] entrada = disco.leer(proceso.getPosicionIndice());
            if (entrada == null) {
                throw new IllegalStateException("El archivo pendiente no tiene una entrada válida en disco.");
            }
            int direccion = Integer.parseInt(entrada[1]);
            int cantidad = Integer.parseInt(entrada[2]);
            int posicionBCP = buscarBloqueMemoria(0, memoria.getInicioUsuario(), getCamposBCP().length);
            int inicioPrograma = buscarBloqueMemoria(memoria.getInicioUsuario(), memoria.getTamañoTotal(), cantidad);
            if (posicionBCP < 0 || inicioPrograma < 0) {
                break;
            }
            for (int i = 0; i < cantidad; i++) {
                memoria.escribirUsuario(inicioPrograma + i, disco.leer(direccion + i));
            }
            bcp.asignarMemoria(inicioPrograma, inicioPrograma + cantidad - 1);
            proceso.asignarPosicionBCP(posicionBCP);
            bcp.setEstadoListo();
            escribirBCPEnMemoria(proceso);
            colaTrabajos.remove();
            planificador.agregarListo(proceso);
        }
        actualizarEnlacesBCP();
    }

    /**
     * Obtiene los campos del BCP.
     *
     * @return etiquetas representadas en kernel.
     */
    private String[] getCamposBCP() {
        return new String[]{"idProceso", "estado", "prioridad", "pc", "inicioMemoria", "finMemoria",
            "ir", "ac", "ax", "bx", "cx", "dx", "flag", "pila1", "pila2", "pila3", "pila4",
            "pila5", "punteroPila", "cpuActual", "tiempoInicio", "tiempoFinal", "tiempoTotalSegundos",
            "direccionSiguienteBCP"};
    }

    /**
     * Escribe el BCP en el kernel.
     *
     * @param proceso proceso cargado en RAM.
     */
    private void escribirBCPEnMemoria(Proceso proceso) {
        String[] nombres = getCamposBCP();
        int posicion = proceso.getPosicionBCP();
        BCP bcp = proceso.getBCP();
        String[] valores = {String.valueOf(bcp.getIdProceso()), bcp.getEstado(), String.valueOf(bcp.getPrioridad()),
            String.valueOf(bcp.getPc()), String.valueOf(bcp.getInicioMemoria()), String.valueOf(bcp.getFinMemoria()),
            bcp.getIrToString(), String.valueOf(bcp.getAc()), String.valueOf(bcp.getAx()), String.valueOf(bcp.getBx()),
            String.valueOf(bcp.getCx()), bcp.getDx(), String.valueOf(bcp.esIgual()), String.valueOf(bcp.getValorPila(0)),
            String.valueOf(bcp.getValorPila(1)), String.valueOf(bcp.getValorPila(2)), String.valueOf(bcp.getValorPila(3)),
            String.valueOf(bcp.getValorPila(4)), String.valueOf(bcp.getPunteroPila()), String.valueOf(bcp.getCpuActual()),
            String.valueOf(bcp.getTiempoInicio()), String.valueOf(bcp.getTiempoFinal()),
            String.valueOf(bcp.getTiempoTotalSegundos()), String.valueOf(bcp.getDireccionSiguienteBCP())};
        for (int i = 0; i < nombres.length; i++) {
            memoria.escribirKernel(posicion + i, nombres[i], valores[i]);
        }
    }

    /**
     * Actualiza los enlaces de los BCP en RAM.
     */
    private void actualizarEnlacesBCP() {
        for (Proceso proceso : procesosRegistrados) {
            if (proceso.getPosicionBCP() < 0) {
                proceso.getBCP().setDireccionSiguienteBCP(-1);
                continue;
            }
            int siguiente = -1;
            for (Proceso otro : procesosRegistrados) {
                int posicion = otro.getPosicionBCP();
                if (posicion > proceso.getPosicionBCP()) {
                    if (siguiente == -1 || posicion < siguiente) {
                        siguiente = posicion;
                    }
                }
            }
            proceso.getBCP().setDireccionSiguienteBCP(siguiente);
            escribirBCPEnMemoria(proceso);
        }
    }

    /**
     * Obtiene el peso de una instrucción.
     *
     * @param instruccion instrucción preparada.
     * @return peso fijo en segundos.
     */
    private int obtenerPeso(String[] instruccion) {
        switch (instruccion[0]) {
            case "MOV":
            case "INC":
            case "DEC":
            case "SWAP":
            case "PUSH":
            case "POP":
                return 1;
            case "LOAD":
            case "STORE":
            case "CMP":
            case "JMP":
            case "JE":
            case "JNE":
                return 2;
            case "ADD":
            case "SUB":
            case "PARAM":
                return 3;
            case "INT":
                if ("10H".equals(instruccion[1]) || "20H".equals(instruccion[1])) {
                    return 2;
                }
                if ("21H".equals(instruccion[1])) {
                    return 5;
                }
                if ("09H".equals(instruccion[1])) {
                    throw new IllegalStateException("INT 09H no tiene un peso fijo; depende de la entrada del usuario.");
                }
                throw new IllegalArgumentException("Interrupción desconocida: " + instruccion[1] + ".");
            default:
                throw new IllegalArgumentException("No se conoce el peso de " + instruccion[0] + ".");
        }
    }

    /**
     * Ejecuta una instrucción completa.
     *
     * @param instruccion instrucción cuyo peso terminó.
     * @return salida de pantalla, o null.
     */
    private String ejecutarInstruccion(String[] instruccion) {
        BCP bcp = getActual().getBCP();
        CPU cpu = getCPU();
        int siguientePc = cpu.getPc() + 1;
        String salida = null;
        switch (instruccion[0]) {
            case "JMP":
                siguientePc = calcularDestinoSalto(Integer.parseInt(instruccion[1]));
                break;
            case "JE":
                if (cpu.esIgual()) {
                    siguientePc = calcularDestinoSalto(Integer.parseInt(instruccion[1]));
                }
                break;
            case "JNE":
                if (!cpu.esIgual()) {
                    siguientePc = calcularDestinoSalto(Integer.parseInt(instruccion[1]));
                }
                break;
            case "PARAM":
                int[] valores = new int[instruccion.length - 1];
                for (int i = 0; i < valores.length; i++) {
                    valores[i] = Integer.parseInt(instruccion[i + 1]);
                }
                if (bcp.getCantidadEnPila() + valores.length > bcp.getCapacidadPila()) {
                    throw new IllegalStateException("Desbordamiento de pila: no hay espacio para los " + valores.length + " parámetros. Capacidad máxima de 5 valores.");
                }
                for (int valor : valores) {
                    bcp.apilar(valor);
                }
                break;
            case "PUSH":
                bcp.apilar(cpu.leerRegistro(instruccion[1]));
                break;
            case "POP":
                cpu.escribirRegistro(instruccion[1], bcp.desapilar());
                break;
            case "INT":
                switch (instruccion[1]) {
                    case "20H":
                        bcp.setEstadoTerminado();
                        break;
                    case "10H":
                        salida = cpu.getDx();
                        break;
                    default:
                        throw new IllegalArgumentException("La interrupción INT " + instruccion[1] + " todavía no está implementada.");
                }
                break;
            default:
                cpu.ejecutarInstruccion(instruccion);
                break;
        }
        cpu.setPc(siguientePc);
        return salida;
    }

    /**
     * Calcula la dirección de un salto.
     *
     * @param desplazamiento desplazamiento desde PC.
     * @return destino dentro del programa.
     */
    private int calcularDestinoSalto(int desplazamiento) {
        BCP bcp = getActual().getBCP();
        int destino = getCPU().getPc() + desplazamiento;
        if (destino < bcp.getInicioMemoria() || destino > bcp.getFinMemoria()) {
            throw new IllegalStateException("Salto fuera de los límites del programa: dirección " + destino
                    + ". Rango permitido: " + bcp.getInicioMemoria() + " a " + bcp.getFinMemoria() + ".");
        }
        return destino;
    }

    /**
     * Comprueba si quedan instrucciones.
     *
     * @param bcp contexto del proceso.
     * @return false solo si PC está en fin más uno.
     */
    private boolean tieneInstrucciones(BCP bcp) {
        if (bcp.getPc() < bcp.getInicioMemoria() || bcp.getPc() > bcp.getFinMemoria() + 1) {
            throw new IllegalStateException("El PC está fuera de los límites del programa.");
        }
        return bcp.getPc() <= bcp.getFinMemoria();
    }

    /**
     * Inicia una espera de teclado.
     */
    private void solicitarEntradaTeclado() {
        Proceso proceso = getActual();
        BCP bcp = proceso.getBCP();
        bcp.setEstadoBloqueado();
        proceso.iniciarEsperaTeclado(System.nanoTime());
        despachador.retirarActual();
        instruccionPendiente = null;
        segundosPendientes = 0;
        escribirBCPEnMemoria(proceso);
    }

    /**
     * Finaliza y libera un proceso.
     *
     * @param proceso proceso que termina.
     */
    private void finalizarProceso(Proceso proceso) {
        BCP bcp = proceso.getBCP();
        boolean esActual = proceso == getActual();
        int posicion = proceso.getPosicionBCP();
        int cantidadCampos = getCamposBCP().length;
        if (posicion < 0) {
            throw new IllegalStateException("El proceso no tiene memoria asignada para liberar.");
        }
        bcp.setEstadoTerminado();
        if (esActual) {
            despachador.retirarActual();
            instruccionPendiente = null;
            segundosPendientes = 0;
        }
        memoria.liberarUsuario(bcp.getInicioMemoria(), bcp.getAlcance());
        memoria.liberarKernel(posicion, cantidadCampos);
        proceso.liberarPosicionBCP();
        bcp.setDireccionSiguienteBCP(-1);
        actualizarEnlacesBCP();
        cargarPendientesEnMemoria();
    }

    /**
     * Reinicia los recursos.
     *
     * @param tamañoMemoria posiciones de RAM.
     * @param tamañoDisco posiciones de disco.
     */
    private void reiniciarRecursos(int tamañoMemoria, int tamañoDisco) {
        if (tamañoMemoria < 128 || tamañoMemoria % 4 != 0) {
            throw new IllegalArgumentException("La RAM debe ser al menos 128 y múltiplo de 4.");
        }
        int tamañoKernel = tamañoMemoria / 4;
        int tamañoIndices = tamañoDisco / 20;
        int tamañoMemoriaVirtual = tamañoDisco / 8;
        Memoria nuevaMemoria = new Memoria(tamañoMemoria, tamañoKernel);
        Disco nuevoDisco = new Disco(tamañoDisco, tamañoIndices, tamañoDisco - tamañoMemoriaVirtual);
        CargadorArchivos nuevoCargador = new CargadorArchivos();
        Planificador nuevoPlanificador = new Planificador();
        Despachador nuevoDespachador = new Despachador(new CPU());
        memoria = nuevaMemoria;
        disco = nuevoDisco;
        cargador = nuevoCargador;
        planificador = nuevoPlanificador;
        despachador = nuevoDespachador;
        procesosRegistrados = new ArrayList<>();
        colaTrabajos = new ArrayDeque<>();
        instruccionPendiente = null;
        segundosPendientes = 0;
    }
}
